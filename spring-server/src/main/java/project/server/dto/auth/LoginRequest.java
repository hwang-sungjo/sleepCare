package project.server.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "로그인 요청 DTO")
public class LoginRequest {

    @NotBlank(message = "userId: {NotBlank}")
    @Schema(description = "로그인 아이디(현재 서비스에서는 닉네임 기반)", example = "sleepy_user")
    private String userId;

    @NotBlank(message = "password: {NotBlank}")
    @Schema(description = "로그인 비밀번호", example = "Password123!")
    private String password;

}
