package project.server.dto.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class GetAlarmResponse {

    /** Goal wake time formatted as HH:mm */
    private String baseWakeTime;
    /** Algorithm-selected instant within the adaptive window (may be null if not computed yet) */
    private Instant dynamicWakeAt;
    private Boolean adaptiveEnabled;
    private Integer windowMinutesBefore;
}
