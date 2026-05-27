package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import project.server.dao.DailyHealthSummaryRepository;
import project.server.dao.HeartRateRepository;
import project.server.dao.HrvRepository;
import project.server.dao.RealtimeMetricRepository;
import project.server.dao.SleepStageRepository;
import project.server.dao.SpO2Repository;
import project.server.dao.entity.RealtimeMetricEntity;
import project.server.entity.DailyHealthSummary;
import project.server.entity.HeartRate;
import project.server.entity.Hrv;
import project.server.entity.SleepStage;
import project.server.entity.SpO2;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 사용자 수면/생체/환경 데이터를 Bedrock RAG 입력용 한국어 컨텍스트 문자열로 직렬화한다.
 *
 * <p>
 * 최신 {@link DailyHealthSummary} 행(KST 오늘부터 7일 역순 탐색)을 기준일로 잡고,
 * 같은 일자의 {@link SleepStage}/{@link HeartRate}/{@link SpO2}/{@link Hrv} 핵심 통계,
 * 그리고 최근 {@link RealtimeMetricEntity} 12건의 환경 평균을 한 줄씩 정리한다.
 * 행이 없으면 "데이터 없음" 표현을 명시하여 모델이 누락을 인지할 수 있게 한다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMetricsSummaryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int RECENT_REALTIME_LIMIT = 12;
    private static final int MAX_LOOKBACK_DAYS = 6;

    private final DailyHealthSummaryRepository summaryRepository;
    private final SleepStageRepository sleepStageRepository;
    private final HeartRateRepository heartRateRepository;
    private final SpO2Repository spo2Repository;
    private final HrvRepository hrvRepository;
    private final RealtimeMetricRepository realtimeMetricRepository;

    /**
     * Bedrock 호출 시 사용자 메시지 앞쪽에 붙이는 컨텍스트 문자열.
     * 항상 비어 있지 않은 문자열을 반환한다 (요약 행이 없을 때도 그 사실을 명시).
     */
    public String buildContext(long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("[사용자 컨텍스트]\n");
        sb.append("- userId: ").append(userId).append('\n');

        Optional<DailyHealthSummary> latest = findLatestSummary(userId);
        if (latest.isEmpty()) {
            sb.append("- 최근 7일 내 daily_health_summary 행 없음\n");
            appendEnvironment(sb, userId);
            return sb.toString();
        }

        DailyHealthSummary s = latest.get();
        LocalDate recordDate = s.getRecordDate();
        sb.append("- 기준 일자(KST): ").append(recordDate).append('\n');
        sb.append("- 수면 효율: ").append(s.getEfficiency()).append("%\n");
        sb.append("- 수면 시간(분): ").append(s.getMinutesAsleep())
                .append(" / 입침 시간: ").append(s.getTimeInBed())
                .append(" / 깸: ").append(s.getMinutesAwake()).append('\n');
        sb.append("- 단계별 분: deep=").append(s.getDeepMins())
                .append(", light=").append(s.getLightMins())
                .append(", rem=").append(s.getRemMins())
                .append(", wake=").append(s.getWakeMins()).append('\n');
        sb.append("- 호흡수: ").append(formatDouble(s.getBreathingRate()))
                .append(" 회/분, 피부온 편차: ").append(formatDouble(s.getSkinTempRelative())).append('\n');
        sb.append("- 본수면 구간: ").append(s.getStartTime()).append(" ~ ").append(s.getEndTime()).append('\n');

        appendStageBreakdown(sb, userId, recordDate);
        appendHeartRate(sb, userId, recordDate);
        appendSpo2(sb, userId, recordDate);
        appendHrv(sb, userId, recordDate);
        appendEnvironment(sb, userId);

        return sb.toString();
    }

    private Optional<DailyHealthSummary> findLatestSummary(long userId) {
        LocalDate today = LocalDate.now(KST);
        for (int i = 0; i <= MAX_LOOKBACK_DAYS; i++) {
            Optional<DailyHealthSummary> hit = summaryRepository.findByUserIdAndRecordDate(userId, today.minusDays(i));
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }

    private void appendStageBreakdown(StringBuilder sb, long userId, LocalDate recordDate) {
        List<SleepStage> stages = sleepStageRepository
                .findByUserIdAndRecordDateOrderByStartTimeAsc(userId, recordDate);
        if (stages.isEmpty()) {
            sb.append("- 수면 단계 타임라인: 데이터 없음\n");
            return;
        }
        Map<String, Long> counts = new TreeMap<>();
        long totalSec = 0L;
        for (SleepStage st : stages) {
            String level = Objects.toString(st.getStageLevel(), "unknown").toLowerCase(Locale.ROOT);
            long sec = st.getDurationSeconds() == null ? 0L : st.getDurationSeconds().longValue();
            counts.merge(level, sec, Long::sum);
            totalSec += sec;
        }
        sb.append("- 수면 단계 누적(분): ");
        boolean first = true;
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append('=').append(e.getValue() / 60L);
            first = false;
        }
        sb.append(" (전체 약 ").append(totalSec / 60L).append("분)\n");
    }

    private void appendHeartRate(StringBuilder sb, long userId, LocalDate recordDate) {
        List<HeartRate> rows = heartRateRepository.findByUserIdAndRecordDateOrderByRecordTimeAsc(userId, recordDate);
        if (rows.isEmpty()) {
            sb.append("- 심박(BPM): 데이터 없음\n");
            return;
        }
        IntSummaryStatistics st = rows.stream()
                .map(HeartRate::getBpm)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .summaryStatistics();
        if (st.getCount() == 0L) {
            sb.append("- 심박(BPM): 데이터 없음\n");
            return;
        }
        sb.append("- 심박(BPM): 평균 ").append(Math.round(st.getAverage()))
                .append(", 최저 ").append(st.getMin())
                .append(", 최고 ").append(st.getMax())
                .append(", 표본 ").append(st.getCount()).append("분\n");
    }

    private void appendSpo2(StringBuilder sb, long userId, LocalDate recordDate) {
        List<SpO2> rows = spo2Repository.findByUserIdAndRecordDateOrderByRecordTimeAsc(userId, recordDate);
        if (rows.isEmpty()) {
            sb.append("- SpO2(%): 데이터 없음\n");
            return;
        }
        DoubleSummaryStatistics st = rows.stream()
                .map(SpO2::getSpo2Value)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        if (st.getCount() == 0L) {
            sb.append("- SpO2(%): 데이터 없음\n");
            return;
        }
        sb.append("- SpO2(%): 평균 ").append(formatDouble(st.getAverage()))
                .append(", 최저 ").append(formatDouble(st.getMin()))
                .append(", 표본 ").append(st.getCount()).append("분\n");
    }

    private void appendHrv(StringBuilder sb, long userId, LocalDate recordDate) {
        List<Hrv> rows = hrvRepository.findByUserIdAndRecordDateOrderByRecordTimeAsc(userId, recordDate);
        if (rows.isEmpty()) {
            sb.append("- HRV(rmssd): 데이터 없음\n");
            return;
        }
        DoubleSummaryStatistics st = rows.stream()
                .map(Hrv::getRmssdValue)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        if (st.getCount() == 0L) {
            sb.append("- HRV(rmssd): 데이터 없음\n");
            return;
        }
        sb.append("- HRV(rmssd): 평균 ").append(formatDouble(st.getAverage()))
                .append(", 표본 ").append(st.getCount()).append("분\n");
    }

    private void appendEnvironment(StringBuilder sb, long userId) {
        List<RealtimeMetricEntity> rows = realtimeMetricRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, RECENT_REALTIME_LIMIT));
        if (rows.isEmpty()) {
            sb.append("- 실시간 환경(최근 ").append(RECENT_REALTIME_LIMIT).append("건): 데이터 없음\n");
            return;
        }
        double avgTemp = mean(rows, RealtimeMetricEntity::getTemperature);
        double avgHum = mean(rows, RealtimeMetricEntity::getHumidity);
        double avgLux = mean(rows, RealtimeMetricEntity::getIlluminance);
        sb.append("- 실시간 환경(최근 ").append(rows.size()).append("건 평균): ")
                .append("온도 ").append(formatDouble(avgTemp)).append("℃, ")
                .append("습도 ").append(formatDouble(avgHum)).append("%, ")
                .append("조도 ").append(formatDouble(avgLux)).append("lx\n");
    }

    private static double mean(List<RealtimeMetricEntity> rows,
            java.util.function.Function<RealtimeMetricEntity, Double> getter) {
        return rows.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0d);
    }

    private static String formatDouble(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
