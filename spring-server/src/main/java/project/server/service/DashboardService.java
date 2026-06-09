package project.server.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import project.server.dao.DailyHealthSummaryRepository;
import project.server.dto.ai.CitationItem;
import project.server.dto.dashboard.DailySleepRecordResponse;
import project.server.dto.dashboard.GetSleepDashboardResponse;
import project.server.dto.dashboard.GetWeeklySleepHistoryResponse;
import project.server.entity.DailyHealthSummary;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 홈 대시보드에 노출되는 수면 효율·수면 시간·AI 조언 조합.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] KST_DAY_LABELS = {"일", "월", "화", "수", "목", "금", "토"};

    private final DailyHealthSummaryRepository summaryRepository;
    private final DashboardAiAdviceService dashboardAiAdviceService;

    /**
     * {@link #findLatestSummary(long)} 결과로 표시 가능한 두 수치와
     * Bedrock 가 생성한 AI 조언 텍스트·KB 인용(실패 시 생략)을 함께 돌려준다.
     */
    public GetSleepDashboardResponse dashboard(long userId) {
        Optional<DailyHealthSummary> latest = findLatestSummary(userId);
        int efficiency = latest.map(DailyHealthSummary::getEfficiency).orElse(0);
        int avgMinutes = latest.map(DailyHealthSummary::getMinutesAsleep).orElse(0);
        Optional<DashboardAiAdviceService.AiAdviceResult> adviceOpt = dashboardAiAdviceService.advise(userId);
        String advice = adviceOpt.map(DashboardAiAdviceService.AiAdviceResult::text).orElse(null);
        List<CitationItem> citations = adviceOpt.map(DashboardAiAdviceService.AiAdviceResult::citations).orElse(null);

        return GetSleepDashboardResponse.builder()
                .sleepEfficiencyPercent(efficiency)
                .averageSleepDurationMinutes(avgMinutes)
                .aiAdvice(advice)
                .citations(citations)
                .build();
    }

    /**
     * KST 기준 오늘을 포함한 최근 7일 구간에서 daily_health_summary 가 있는 날의 상세 수면 기록을 반환한다.
     */
    public GetWeeklySleepHistoryResponse weeklySleepHistory(long userId) {
        LocalDate today = LocalDate.now(KST);
        LocalDate start = today.minusDays(6);
        List<DailySleepRecordResponse> records = summaryRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, start, today)
                .stream()
                .map(this::toDailySleepRecord)
                .toList();

        return GetWeeklySleepHistoryResponse.builder()
                .records(records)
                .build();
    }

    private DailySleepRecordResponse toDailySleepRecord(DailyHealthSummary summary) {
        LocalDate recordDate = summary.getRecordDate();
        String dayLabel = KST_DAY_LABELS[recordDate.getDayOfWeek().getValue() % 7];

        return DailySleepRecordResponse.builder()
                .date(recordDate.toString())
                .dayLabel(dayLabel)
                .sleepEfficiency(summary.getEfficiency())
                .sleepDurationMinutes(summary.getMinutesAsleep())
                .sleepStartTime(summary.getStartTime().format(TIME_FMT))
                .sleepEndTime(summary.getEndTime().format(TIME_FMT))
                .deepMins(summary.getDeepMins())
                .remMins(summary.getRemMins())
                .lightMins(summary.getLightMins())
                .wakeMins(summary.getWakeMins())
                .build();
    }

    /**
     * KST 기준 오늘부터 과거로 하루씩 줄여 가며 탐색해, 요약 행이 있는 첫 날 한 건만 반환한다.
     * (당일 또는 어제 행이 없으면 더 이전 날이 선택될 수 있어 사용자의 "어제"와 다를 수 있다.)
     */
    private Optional<DailyHealthSummary> findLatestSummary(long userId) {
        LocalDate today = LocalDate.now(KST);
        for (int i = 0; i <= 6; i++) {
            Optional<DailyHealthSummary> hit = summaryRepository.findByUserIdAndRecordDate(userId, today.minusDays(i));
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }
}
