package project.server.dto.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
public class PatchAlarmRequest {

    @Nullable
    @Schema(description = "Goal wake time (HH:mm)", example = "07:30")
    private String baseWakeTime;

    @Nullable
    private Boolean adaptiveEnabled;

    @Nullable
    @Min(5)
    @Max(120)
    @Schema(description = "Adaptive window ending at base wake time, in minutes")
    private Integer windowMinutesBefore;

    /** When true, recomputes dynamic wake time using latest Fitbit + sensor snapshots */
    @Nullable
    private Boolean recomputeDynamicNow;
}
