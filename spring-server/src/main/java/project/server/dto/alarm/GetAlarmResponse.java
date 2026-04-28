package project.server.dto.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "알람 조회/수정 응답 DTO")
public class GetAlarmResponse {

    @Schema(description = "사용자가 설정한 목표 기상시간(HH:mm)", example = "07:30")
    private String baseWakeTime;

    @Schema(
            description = "동적 알고리즘이 계산한 실제 기상 시각(UTC Instant, 미계산 시 null)",
            example = "2026-04-29T22:15:00Z",
            nullable = true)
    private Instant dynamicWakeAt;

    @Schema(description = "적응형 알람 활성화 여부", example = "true")
    private Boolean adaptiveEnabled;

    @Schema(description = "기본 기상시각 이전 탐색 윈도우(분)", example = "30")
    private Integer windowMinutesBefore;
}
