package project.server.dto.fitbit;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
public class FitbitOAuthExchangeRequest {

    @NotBlank
    private String code;

    @Nullable
    private String redirectUri;
}
