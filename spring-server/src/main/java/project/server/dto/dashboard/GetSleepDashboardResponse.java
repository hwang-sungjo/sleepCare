package project.server.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "홈 대시보드 수면 요약 응답 DTO")
public class GetSleepDashboardResponse {

    @Schema(
            description = "수면 효율(%) — 해당 summary 행 값. 존재하는 요약 행 하나를 사용하며 과거 평균 등은 아님.",
            example = "92")
    private Integer sleepEfficiencyPercent;

@Schema(description = "수면 요약 행 하나의 수면 시간(분). 대시보드는 최근 존재하는 행 하나를 사용합니다.")
    private Integer averageSleepDurationMinutes;

    @Schema(description = "최근 IoT realtime_metric 에서 계산된 환경 가이드 문구.")
    private String environmentHint;
}
