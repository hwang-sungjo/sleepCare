package project.server.dto.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "알람 조회/수정 응답 DTO")
public class GetAlarmResponse {

    @Schema(description = "오늘 요일 (ISO 기준: 1=월 ... 7=일)", example = "2")
    private Integer todayDayOfWeek;

    @Schema(
            description = "오늘 실제로 사용되는 기상 시각(동적 우선, 없으면 기본 시각 기반)",
            example = "2026-04-29T22:15:00Z",
            nullable = true)
    private Instant todayEffectiveWakeAt;

    @Schema(description = "요일별 알람 목록 (요일당 최대 1개)")
    private List<DailyAlarmItemResponse> alarms;
}
