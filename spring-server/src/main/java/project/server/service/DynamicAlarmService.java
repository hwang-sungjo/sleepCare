package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.dao.AlarmRepository;
import project.server.dao.SleepStageRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.entity.SleepStage;
import project.server.util.AlarmWakeAtHelper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 적응형 알람(dynamic_wake_at) 재계산.
 *
 * <h2>설계 근거 (요약)</h2>
 * <ol>
 * <li><b>적응형 OFF:</b> {@code adaptive_enabled == false} 이면 수면 단계 탐색·윈도 탐색을 하지 않는다.
 * 오늘의 목표 벽시각({@link AlarmWakeAtHelper#todayWakeInstant(java.time.LocalTime, ZoneId)})
 * 과 동등한 순간으로만 맞추고 저장 후 종료한다.</li>
 * <li><b>windowEnd:</b> KST 로 오늘 {@link LocalDate} + {@link AlarmEntity#getBaseWakeTime()} →
 * {@link ZonedDateTime#of(LocalDate, java.time.LocalTime, ZoneId)} 후 {@link Instant}.</li>
 * <li><b>‘이미 울림’:</b>
 * 현재 순간(now) 이 (a) 현재 행의 {@code dynamic_wake_at}, 또는 (b) 오늘 {@code windowEnd}
 * 보다도 늦지 않으면 이미 해당 일정을 지난 것으로 본다(OR 조건).
 * 이 경우 다음 주 같은 요일·같은 base 시각 조합으로 {@code dynamic_wake_at} 을 당긴다.</li>
 * <li><b>창구:</b> {@code windowStart = windowEnd - windowMinutesBefore} (분 차).
 * 검색 폐구간 관점에서 {@link #intervalsOverlap(Instant, Instant, Instant, Instant)} 로
 * 수면 세그먼트 [{@code startTime}, {@code startTime}+{@code durationSeconds}] 과
 * [windowStart, windowEnd] 가 겹치는지 본다.</li>
 * <li><b>수면 행 조회:</b> {@link SleepStageRepository#findByUserIdAndRecordDateOrderByStartTimeAsc(Long, LocalDate)},
 * 레코드일은 KST 의 ‘오늘’.</li>
 * <li><b>얕은 수면:</b> 문자열 레벨이 deep 을 포함하면 제외하고, 나머지는 키워드
 * ({@link #SHALLOW_STAGE_KEYWORDS}) 매칭. DB 의 {@link SleepStage#getStartTime()} 는 이미 Instant.</li>
 * <li><b>후보 순간:</b> 겹침 구간 안에서 가장 빨리 깨어날 수 있는 시각으로
 * {@code max(segStart, windowStart)}(겹치는 경우에 한함) 한 점만 후보 목록에 넣는다.</li>
 * <li><b>최종 선택:</b> 후보 중 {@code >= now} 인 것만 남긴 뒤 가장 이른 순간을 고르고,
 * 없으면 {@code windowEnd}.</li>
 * <li><b>데모:</b> 현재 버전에서는 위 계산 끝에 상수가 true 일 때 결과를 무시하고
 * {@code windowStart}(= 명세의 벽시각 기준 목표 − window 분) 에 고정한다.
 * (‘다 구현 후 마지막에 고정’ 요구 반영)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicAlarmService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    /** Fitbit/API 호환 문자열에서 얕은 수면 후보 판별용 소문자 키워드. deep 포함 문자열은 별도로 배제한다. */
    private static final Set<String> SHALLOW_STAGE_KEYWORDS = Set.of(
            "light", "rem", "restless", "awake", "wake");

    /**
     * true 이면 적응형 단계까지 계산해 본 {@code chosenInstant} 를 버리고
     * 반드시 windowStart 로 저장한다 (데모 목적).
     */
    private static final boolean DEMO_FORCE_DYNAMIC_EQUALS_WINDOW_START = true;

    private final AlarmRepository alarmRepository;
    private final SleepStageRepository sleepStageRepository;
    /** 동적 알람 저장 후 브로커로 라즈베리 일정 MQTT 발행까지 담당. */
    private final MqttAlarmPublisher mqttAlarmPublisher;

    @Transactional
    public void recalculateForUser(Long userId) {
        AlarmEntity alarm = alarmRepository.findByUserIdAndDayOfWeek(userId,
                LocalDate.now(DEFAULT_ZONE).getDayOfWeek().getValue()).orElse(null);
        if (alarm == null) {
            return;
        }

        Instant now = Instant.now();
        int windowMinutes = Objects.requireNonNullElse(alarm.getWindowMinutesBefore(), 30);
        Instant windowEnd = calculateWindowEndInstant(alarm, now);
        Instant windowStart = windowEnd.minus(windowMinutes, ChronoUnit.MINUTES);

        // ① 적응형 비활성: 동적 창 검색 없이 오늘 base 순간만 유지 후 종료.
        if (Boolean.FALSE.equals(alarm.getAdaptiveEnabled())) {
            alarm.setDynamicWakeAt(AlarmWakeAtHelper.todayWakeInstant(alarm.getBaseWakeTime(), DEFAULT_ZONE));
            persistDynamicAlarm(userId, alarm);
            return;
        }

        // ② 이미 알람 시간대를 지나감: 다음 주 같은 요일로 옮김.
        if (hasPassedWakeSchedule(alarm, now, windowEnd)) {
            alarm.setDynamicWakeAt(AlarmWakeAtHelper.nextWeeklyWakeInstant(
                    alarm.getDayOfWeek(), alarm.getBaseWakeTime(), DEFAULT_ZONE));
            persistDynamicAlarm(userId, alarm);
            log.debug("[DynamicAlarmService] user={} already passed wake band → next weekly instant", userId);
            return;
        }

        // ⑤ 수면 단계 목록(KST 오늘, 시작 Instant 오름차순).
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        List<SleepStage> stages =
                sleepStageRepository.findByUserIdAndRecordDateOrderByStartTimeAsc(userId, today);

        // ⑥~⑦: 얕은 구간 후보 순간 후, now 이후만 남겨 최소 선택. 부재 시 windowEnd.
        Instant chosenInstant = stages.stream()
                .map(s -> clippedShallowWakeCandidate(s, windowStart, windowEnd))
                .filter(Objects::nonNull)
                .filter(t -> !t.isBefore(now))
                .min(Comparator.naturalOrder())
                .orElse(windowEnd);

        // ⑨ 데모: 계산 결과를 버리고 base − window 분(= windowStart 순간)에 고정한다.
        if (DEMO_FORCE_DYNAMIC_EQUALS_WINDOW_START) {
            chosenInstant = windowStart;
        }

        alarm.setDynamicWakeAt(chosenInstant);
        persistDynamicAlarm(userId, alarm);
        log.debug("[DynamicAlarmService] user={} dynamicWakeAt={} demoFixedWindowStart={}",
                userId, chosenInstant, DEMO_FORCE_DYNAMIC_EQUALS_WINDOW_START);
    }

    private void persistDynamicAlarm(Long userId, AlarmEntity alarm) {
        alarmRepository.save(alarm);
        mqttAlarmPublisher.publishWakeSchedule(userId, alarm.getDynamicWakeAt());
    }

    /**
     * KST 기준 ‘reference 시점이 속하는 달력의 날’ + {@code baseWakeTime} → 해당 일의 목표 종료 순간 Instant.
     * (설계서의 {@code windowEnd})
     */
    private Instant calculateWindowEndInstant(AlarmEntity alarm, Instant reference) {
        LocalDate today = LocalDate.ofInstant(reference, DEFAULT_ZONE);
        return ZonedDateTime.of(today, alarm.getBaseWakeTime(), DEFAULT_ZONE).toInstant();
    }

    /**
     * 현재 순간이 (1) 현재 행 기준 채택된 알람 순간(dynamic)을 지나쳤거나,
     * (2) 목표 종료 순간(windowEnd, 기본 목표 벽시계) 까지도 지난 경우 true.
     * 둘 중 하나만 해당돼도 ‘이미 울림/일과 종료’로 보고 다음 주로 넘긴다.
     */
    private static boolean hasPassedWakeSchedule(AlarmEntity alarm, Instant now, Instant windowEnd) {
        Instant dynamic = alarm.getDynamicWakeAt();
        boolean passedDynamic = dynamic != null && !now.isBefore(dynamic);
        boolean passedWindowEnd = !now.isBefore(windowEnd);
        return passedDynamic || passedWindowEnd;
    }

    /**
     * ⑥ 세그먼트가 [windowStart, windowEnd] 와 교차하고, 레벨이 얕으면 깨어날 후보 시각 하나를 돌린다.
     *
     * <p>
     * 겹침: 닫힌 구간으로 볼 때 교집합이 비어있지 않을 것.
     * 후보 순간은 요구 명세와 같이 {@code max(segStart, windowStart)} 이나,
     * 이 값이 교집합의 하단과 일치하도록 교차 여부 검사 후에만 반환한다.
     * </p>
     */
    private static Instant clippedShallowWakeCandidate(SleepStage row, Instant windowStart, Instant windowEnd) {
        if (row == null || row.getStageLevel() == null) {
            return null;
        }
        String stageNorm = row.getStageLevel().toLowerCase(Locale.ROOT);
        if (!isShallowStage(stageNorm)) {
            return null;
        }

        Instant segStart = row.getStartTime();
        if (segStart == null) {
            return null;
        }

        long durationSec = row.getDurationSeconds() == null ? 0L : row.getDurationSeconds().longValue();
        Instant segEnd = segStart.plus(Duration.ofSeconds(durationSec));

        if (!intervalsOverlap(segStart, segEnd, windowStart, windowEnd)) {
            return null;
        }

        Instant candidate = segStart.isBefore(windowStart) ? windowStart : segStart;
        if (candidate.isAfter(windowEnd)) {
            return null;
        }
        return candidate;
    }

    /**
     * [aStart, aEnd], [bStart, bEnd] 가 시간축 상 한 점이라도 함께 덮면 true.
     */
    private static boolean intervalsOverlap(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        return !aStart.isAfter(bEnd) && !aEnd.isBefore(bStart);
    }

    /** deep 문자열 포함 시 깊은 수면으로 간주하여 제외, 그 외 키워드 부분 문자열 허용. */
    private static boolean isShallowStage(String stageLower) {
        if (stageLower.contains("deep")) {
            return false;
        }
        return SHALLOW_STAGE_KEYWORDS.stream().anyMatch(stageLower::contains);
    }
}
