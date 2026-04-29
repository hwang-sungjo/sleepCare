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

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyHealthSummaryRepository summaryRepository;
    private final RealtimeMetricRepository realtimeMetricRepository;

    public GetSleepDashboardResponse dashboard(long userId) {
        Optional<DailyHealthSummary> latest = findLatestSummary(userId);
        int efficiency = latest.map(DailyHealthSummary::getEfficiency).orElse(0);
        int avgMinutes = latest.map(DailyHealthSummary::getMinutesAsleep).orElse(0);
        String hint = environmentHint(userId);

        return GetSleepDashboardResponse.builder()
                .sleepEfficiencyPercent(efficiency)
                .averageSleepDurationMinutes(avgMinutes)
                .environmentHint(hint)
                .build();
    }

    /** 가장 최근 7일 안에서 가장 최신 daily summary 한 건을 가져온다. */
    private Optional<DailyHealthSummary> findLatestSummary(long userId) {
        LocalDate today = LocalDate.now(KST);
        for (int i = 0; i <= 6; i++) {
            Optional<DailyHealthSummary> hit =
                    summaryRepository.findByUserIdAndRecordDate(userId, today.minusDays(i));
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }

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
        double avgLux =
                page.stream().map(s -> s.getIlluminance()).filter(java.util.Objects::nonNull).mapToDouble(v -> v)
                        .average().orElse(0d);
        if (avgLux > 200d) {
            return "취침 전 시간대 조도가 높았습니다. 암막 또는 차광 블라인드를 활용하면 멜라토닌 분비에 도움이 됩니다.";
        }
        return "환경 신호가 안정적입니다. 같은 조건으로 수면 루틴을 유지해 보세요.";
    }
}
