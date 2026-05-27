package project.server.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import project.server.dao.DailyHealthSummaryRepository;
import project.server.dao.RealtimeMetricRepository;
import project.server.dto.dashboard.GetSleepDashboardResponse;
import project.server.entity.DailyHealthSummary;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.DoubleSummaryStatistics;
import java.util.Optional;

/**
 * 홈 대시보드에 노출되는 수면 효율·수면 시간·환경 안내 문자열 조합.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyHealthSummaryRepository summaryRepository;
    private final RealtimeMetricRepository realtimeMetricRepository;
    private final DashboardAiAdviceService dashboardAiAdviceService;

    /**
     * {@link #findLatestSummary(long)} 결과로 표시 가능한 두 수치, 최근 IoT 지표 요약 문자열,
     * 그리고 Bedrock 가 생성한 AI 조언 텍스트(실패 시 생략)를 함께 돌려준다.
     */
    public GetSleepDashboardResponse dashboard(long userId) {
        Optional<DailyHealthSummary> latest = findLatestSummary(userId);
        int efficiency = latest.map(DailyHealthSummary::getEfficiency).orElse(0);
        int avgMinutes = latest.map(DailyHealthSummary::getMinutesAsleep).orElse(0);
        String hint = environmentHint(userId);
        String advice = dashboardAiAdviceService.advise(userId).orElse(null);

        return GetSleepDashboardResponse.builder()
                .sleepEfficiencyPercent(efficiency)
                .averageSleepDurationMinutes(avgMinutes)
                .environmentHint(hint)
                .aiAdvice(advice)
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

    /**
     * 최근 실시간 센서 샘플(최대 12건)의 습도·조도 평균으로 짧은 권장 문구를 선택한다.
     */
    private String environmentHint(long userId) {
        var page = realtimeMetricRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 12));
        if (page.isEmpty()) {
            return "실내 데이터가 아직 충분하지 않습니다.";
        }
        DoubleSummaryStatistics hum = page.stream()
                .map(s -> s.getHumidity())
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        if (hum.getCount() == 0d) {
            return "실내 데이터가 아직 충분하지 않습니다.";
        }
        double avgHum = hum.getAverage();
        if (avgHum > 70d) {
            return "실내 평균 습도가 높습니다. 제습 또는 환기로 60~65% 근처를 유지하면 숙면에 도움이 될 수 있습니다.";
        }
        if (avgHum < 35d) {
            return "실내 공기가 다소 건조합니다. 가습을 조금 높여 보세요.";
        }
        double avgLux = page.stream().map(s -> s.getIlluminance()).filter(java.util.Objects::nonNull)
                .mapToDouble(v -> v)
                .average().orElse(0d);
        if (avgLux > 200d) {
            return "취침 전 시간대 조도가 높았습니다. 암막 또는 차광 블라인드를 활용하면 멜라토닌 분비에 도움이 됩니다.";
        }
        return "환경 신호가 안정적입니다. 같은 조건으로 수면 루틴을 유지해 보세요.";
    }
}
