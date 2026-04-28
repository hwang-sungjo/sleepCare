package project.server.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GetSleepDashboardResponse {

    private Integer sleepEfficiencyPercent;
    private Integer averageSleepDurationMinutes;
    /** Short guidance text combining recent sensor averages (humidity/light) */
    private String environmentHint;
}
