package project.server.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "최근 7일 중 하루의 수면 상세 기록")
public class DailySleepRecordResponse {

    @Schema(description = "기록 날짜 (YYYY-MM-DD, KST)", example = "2026-05-28")
    private String date;

    @Schema(description = "요일 단축 라벨", example = "수")
    private String dayLabel;

    @Schema(description = "수면 효율(%)", example = "88")
    private int sleepEfficiency;

    @Schema(description = "총 수면 시간(분)", example = "420")
    private int sleepDurationMinutes;

    @Schema(description = "취침 시각 (HH:mm, KST)", example = "23:15")
    private String sleepStartTime;

    @Schema(description = "기상 시각 (HH:mm, KST)", example = "07:05")
    private String sleepEndTime;

    @Schema(description = "깊은 수면(분)", example = "78")
    private int deepMins;

    @Schema(description = "REM 수면(분)", example = "95")
    private int remMins;

    @Schema(description = "얕은 수면(분)", example = "210")
    private int lightMins;

    @Schema(description = "중간 각성(분)", example = "18")
    private int wakeMins;
}
