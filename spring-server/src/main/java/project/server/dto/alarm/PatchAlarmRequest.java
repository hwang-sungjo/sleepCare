package project.server.dto.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "알람 설정 변경 요청 DTO")
public class PatchAlarmRequest {

    @NotNull
    @Min(1)
    @Max(7)
    @Schema(description = "변경할 요일 (ISO 기준: 1=월 ... 7=일)", example = "1")
    private Integer dayOfWeek;

    @Nullable
    @Schema(description = "목표 기상 시간(HH:mm 형식)", example = "07:30", nullable = true)
    private String baseWakeTime;

    @Nullable
    @Schema(description = "적응형 알람 사용 여부", example = "true", nullable = true)
    private Boolean adaptiveEnabled;

    @Nullable
    @Min(5)
    @Max(120)
    @Schema(description = "기본 기상시간 이전 탐색 윈도우(분)", example = "30", nullable = true)
    private Integer windowMinutesBefore;

    @Nullable
    @Schema(description = "true면 최신 데이터 기반으로 동적 알람 시각을 즉시 재계산", example = "false", nullable = true)
    private Boolean recomputeDynamicNow;
}
