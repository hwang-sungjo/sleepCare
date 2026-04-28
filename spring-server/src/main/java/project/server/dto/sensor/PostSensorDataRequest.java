package project.server.dto.sensor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class PostSensorDataRequest {

    @NotNull
    private Long userId;

    private Instant recordedAt;

    @Nullable
    private String deviceId;

    @Nullable
    private Double illuminance;

    @Nullable
    private Double temperature;

    @Nullable
    private Double humidity;

    @Schema(hidden = true)
    public Instant resolveRecordedAt() {
        return recordedAt != null ? recordedAt : Instant.now();
    }
}
