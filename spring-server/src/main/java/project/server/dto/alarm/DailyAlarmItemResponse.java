package project.server.dto.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "특정 요일의 알람 설정/동적 계산 결과")
public class DailyAlarmItemResponse {

    @Schema(description = "요일 (ISO 기준: 1=월 ... 7=일)", example = "1")
    private Integer dayOfWeek;

    @Schema(description = "사용자가 설정한 기본 기상시간(HH:mm)", example = "07:30")
    private String baseWakeTime;

    @Schema(description = "해당 요일에 계산되어 저장된 동적 기상 시각(한국 벽시계 LocalDateTime)", example = "2026-04-30T07:30:00")
    private LocalDateTime dynamicWakeAt;

    @Schema(description = "적응형 알람 활성화 여부", example = "true")
    private Boolean adaptiveEnabled;

    @Schema(description = "기본 기상시간 이전 탐색 윈도우(분)", example = "30")
    private Integer windowMinutesBefore;
}
