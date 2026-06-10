package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.dao.AlarmRepository;
import project.server.dao.HeartRateRepository;
import project.server.dao.RealtimeMetricRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.dao.entity.RealtimeMetricEntity;
import project.server.entity.HeartRate;
import project.server.util.AlarmWakeAtHelper;
import project.server.util.EnvironmentalWakeEvaluator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 적응형 알람(dynamic_wake_at) 재계산.
 *
 * <h2>4요인 기상 환경 스코어링 알고리즘</h2>
 * <p>기상 목표 시각(base_wake_time) 기준 {@value LOOKBACK_MINUTES}분 전부터의
 * 심박수·조도·온도·습도 데이터를 분 단위로 종합해 기상 쾌적도 점수를 계산하고,
 * 점수에 따라 탐색 창(window) 내 최적 기상 시각을 결정한다.</p>
 *
 * <ol>
 * <li>적응형 OFF → base_wake_time 그대로.</li>
 * <li>이미 지남 → 다음 주 동일 요일.</li>
 * <li>센서 데이터 조회: HeartRate(Fitbit 1분 단위) + RealtimeMetric(IoT 환경 지표).</li>
 * <li>분 단위 4요인 점수 계산 후 단순 평균 집계.</li>
 * <li>집계 점수 → 탐색 창 내 기상 오프셋 결정.
 *     점수 높을수록(기상 환경 좋을수록) 창의 앞쪽(더 이른 시각)에서 기상.</li>
 * <li>데이터 없음 → 점수 50(중립) → 창 중간 지점 폴백.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicAlarmService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    /** HR 데이터 없을 때 사용하는 수면 최저 심박 기본값 (bpm). */
    private static final int DEFAULT_SLEEP_MIN_BPM = 55;

    /** 기상 목표 시각 기준 데이터 조회 선행 시간 (분). */
    private static final int LOOKBACK_MINUTES = 60;

    private final AlarmRepository alarmRepository;
    private final HeartRateRepository heartRateRepository;
    private final RealtimeMetricRepository realtimeMetricRepository;
    /** lambda 프로파일에서는 빈 없음 → Optional. */
    private final Optional<MqttAlarmPublisher> mqttAlarmPublisher;

    @Transactional
    public void recalculateForUserAndDay(Long userId, int dayOfWeek) {
        AlarmEntity alarm = alarmRepository.findByUserIdAndDayOfWeek(userId, dayOfWeek).orElse(null);
        if (alarm == null) return;

        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        LocalDateTime windowEnd = calculateWindowEndLocal(alarm, now);

        // ① 적응형 비활성: base_wake_time 그대로 유지 후 종료.
        if (Boolean.FALSE.equals(alarm.getAdaptiveEnabled())) {
            alarm.setDynamicWakeAt(AlarmWakeAtHelper.nearestUpcomingWakeAt(
                    alarm.getDayOfWeek(), alarm.getBaseWakeTime(), DEFAULT_ZONE));
            persistDynamicAlarm(userId, alarm);
            return;
        }

        // ② 이미 알람 시간대를 지나감: 다음 주 같은 요일로 옮김.
        if (hasPassedWakeSchedule(alarm, now, windowEnd)) {
            alarm.setDynamicWakeAt(AlarmWakeAtHelper.nextWeeklyWakeAt(
                    alarm.getDayOfWeek(), alarm.getBaseWakeTime(), DEFAULT_ZONE));
            persistDynamicAlarm(userId, alarm);
            log.debug("[DynamicAlarmService] user={} already passed wake band → next weekly wake", userId);
            return;
        }

        // ③ 탐색 창 계산.
        int windowMinutes = Objects.requireNonNullElse(alarm.getWindowMinutesBefore(), 30);
        LocalDateTime windowStart = windowEnd.minusMinutes(windowMinutes);

        // ④ 센서 데이터 조회: 현재 시점(now) 기준으로 과거 60분(LOOKBACK_MINUTES) 조회.
        // 스케줄러가 알람 1시간 전에 동작하므로, 당시의 최신 1시간 데이터를 기반으로 기상 환경을 평가한다.
        LocalDate today = now.toLocalDate();
        LocalDateTime lookbackEnd = now;
        LocalDateTime lookbackStart = now.minusMinutes(LOOKBACK_MINUTES);

        List<HeartRate> hrData = heartRateRepository.findByUserIdAndRecordTimeBetweenOrderByRecordTimeAsc(
                userId, lookbackStart, lookbackEnd);
        List<RealtimeMetricEntity> iotData = realtimeMetricRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, lookbackStart, lookbackEnd);

        // ⑤ 수면 HR 베이스라인: 어제 최저 심박 (없으면 기본값 사용).
        int sleepMinBpm = heartRateRepository.findMinBpmByUserIdAndRecordDate(userId, today.minusDays(1))
                .orElse(DEFAULT_SLEEP_MIN_BPM);

        // ⑥ 4요인 종합 점수 계산 (데이터 없으면 50점 = 중립).
        double aggregateScore = computeAggregateScore(userId, hrData, iotData, sleepMinBpm);

        // ⑦ 점수 → 창 내 기상 시각 결정.
        // ratio 0.0(점수 100) → 창 앞쪽 기상, 1.0(점수 0) → 창 끝 기상.
        double ratio = Math.max(0.0, Math.min(1.0, (100.0 - aggregateScore) / 100.0));
        LocalDateTime proposedWakeAt = windowStart.plusMinutes((long) (ratio * windowMinutes));

        // 제안 시각이 이미 지났으면: 창 안에 아직 있으면 즉시, 아니면 windowEnd.
        LocalDateTime chosenAt;
        if (proposedWakeAt.isBefore(now)) {
            chosenAt = now.isBefore(windowEnd) ? now : windowEnd;
        } else {
            chosenAt = proposedWakeAt;
        }

        alarm.setDynamicWakeAt(chosenAt);
        persistDynamicAlarm(userId, alarm);
        log.debug("[DynamicAlarmService] user={} score={} windowStart={} proposedWakeAt={} chosenAt={}",
                userId, String.format("%.1f", aggregateScore), windowStart, proposedWakeAt, chosenAt);
    }

    /**
     * HR 분 단위 데이터를 기준으로 IoT 데이터와 매칭해 4요인 평균 점수를 산출한다.
     *
     * <ul>
     *   <li>HR + IoT 모두 있음: 4요인 combinedScore 평균.</li>
     *   <li>HR 없고 IoT만 있음: 3요인 combinedScoreWithoutHr 평균 (HR 가중치 분배).</li>
     *   <li>둘 다 없음: 50.0 반환 (창 중간 지점 폴백).</li>
     * </ul>
     */
    private double computeAggregateScore(Long userId,
            List<HeartRate> hrData, List<RealtimeMetricEntity> iotData, int sleepMinBpm) {

        if (hrData.isEmpty() && iotData.isEmpty()) {
            log.debug("[DynamicAlarmService] user={} no sensor data → neutral score 50", userId);
            return 50.0;
        }

        double sumScore = 0.0;
        int count = 0;

        // HR 기준으로 1분 단위 4요인 점수 계산.
        for (HeartRate hr : hrData) {
            if (hr.getBpm() == null) continue;

            RealtimeMetricEntity iot = EnvironmentalWakeEvaluator.findNearestIot(iotData, hr.getRecordTime());

            double hrScore   = EnvironmentalWakeEvaluator.scoreHr(hr.getBpm(), sleepMinBpm);
            double luxScore  = iot != null && iot.getIlluminance() != null
                    ? EnvironmentalWakeEvaluator.scoreLux(iot.getIlluminance())
                    : EnvironmentalWakeEvaluator.NEUTRAL_SCORE;
            double tempScore = iot != null && iot.getTemperature() != null
                    ? EnvironmentalWakeEvaluator.scoreTemp(iot.getTemperature())
                    : EnvironmentalWakeEvaluator.NEUTRAL_SCORE;
            double humScore  = iot != null && iot.getHumidity() != null
                    ? EnvironmentalWakeEvaluator.scoreHumidity(iot.getHumidity())
                    : EnvironmentalWakeEvaluator.NEUTRAL_SCORE;

            double momentScore = EnvironmentalWakeEvaluator.combinedScore(hrScore, luxScore, tempScore, humScore);
            log.debug("[DynamicAlarmService] user={} t={} hrScore={} luxScore={} tempScore={} humScore={} moment={}",
                    userId, hr.getRecordTime(),
                    String.format("%.1f", hrScore), String.format("%.1f", luxScore),
                    String.format("%.1f", tempScore), String.format("%.1f", humScore),
                    String.format("%.1f", momentScore));

            sumScore += momentScore;
            count++;
        }

        // HR 없고 IoT 데이터만 있는 경우: 3요인으로 정규화.
        if (count == 0) {
            for (RealtimeMetricEntity iot : iotData) {
                double luxScore  = iot.getIlluminance() != null
                        ? EnvironmentalWakeEvaluator.scoreLux(iot.getIlluminance())
                        : EnvironmentalWakeEvaluator.NEUTRAL_SCORE;
                double tempScore = iot.getTemperature() != null
                        ? EnvironmentalWakeEvaluator.scoreTemp(iot.getTemperature())
                        : EnvironmentalWakeEvaluator.NEUTRAL_SCORE;
                double humScore  = iot.getHumidity() != null
                        ? EnvironmentalWakeEvaluator.scoreHumidity(iot.getHumidity())
                        : EnvironmentalWakeEvaluator.NEUTRAL_SCORE;

                sumScore += EnvironmentalWakeEvaluator.combinedScoreWithoutHr(luxScore, tempScore, humScore);
                count++;
            }
        }

        return count > 0 ? Math.max(0.0, Math.min(100.0, sumScore / count)) : 50.0;
    }

    private void persistDynamicAlarm(Long userId, AlarmEntity alarm) {
        alarmRepository.save(alarm);
        mqttAlarmPublisher.ifPresent(pub -> pub.publishWakeSchedule(userId, alarm.getDynamicWakeAt()));
    }

    private LocalDateTime calculateWindowEndLocal(AlarmEntity alarm, LocalDateTime reference) {
        LocalDate date = reference.toLocalDate();
        int currentDow = date.getDayOfWeek().getValue();
        int targetDow = alarm.getDayOfWeek();
        
        if (currentDow != targetDow) {
            int diff = targetDow - currentDow;
            if (diff > 3) diff -= 7;
            if (diff < -3) diff += 7;
            date = date.plusDays(diff);
        }
        return LocalDateTime.of(date, alarm.getBaseWakeTime());
    }

    private static boolean hasPassedWakeSchedule(AlarmEntity alarm, LocalDateTime now, LocalDateTime windowEnd) {
        LocalDateTime dynamic = alarm.getDynamicWakeAt();
        boolean passedDynamic = dynamic != null && !now.isBefore(dynamic);
        boolean passedWindowEnd = !now.isBefore(windowEnd);
        return passedDynamic || passedWindowEnd;
    }
}
