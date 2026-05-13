package project.server.dto.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "알람 조회/수정 응답 DTO")
public class GetAlarmResponse {

    @Schema(description = "오늘 요일 (ISO 기준: 1=월 ... 7=일)", example = "2")
    private Integer todayDayOfWeek;

    @Schema(
            description = "오늘 요일 알람 행이 있으면 그 행의 dynamic_wake_at (한국 벽시계 LocalDateTime).",
            example = "2026-04-30T07:30:00",
            nullable = true)
    private LocalDateTime todayEffectiveWakeAt;

    @Schema(description = "요일별 알람 목록 (요일당 최대 1개)")
    private List<DailyAlarmItemResponse> alarms;
}
