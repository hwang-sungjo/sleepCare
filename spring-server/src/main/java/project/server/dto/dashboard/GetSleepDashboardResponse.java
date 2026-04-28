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

    @Schema(description = "수면 효율(%)", example = "92")
    private Integer sleepEfficiencyPercent;

    @Schema(description = "평균 수면 시간(분)", example = "440")
    private Integer averageSleepDurationMinutes;

    @Schema(description = "환경 데이터 기반 수면 가이드 문구", example = "실내 평균 습도가 높습니다. 제습을 권장합니다.")
    private String environmentHint;
}
