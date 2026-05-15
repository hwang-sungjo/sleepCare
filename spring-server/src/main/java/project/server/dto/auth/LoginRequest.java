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

    @NotBlank(message = "nickname: {NotBlank}")
    @Schema(description = "로그인 닉네임", example = "sleepy_user")
    private String nickname;

    @NotBlank(message = "password: {NotBlank}")
    @Schema(description = "로그인 비밀번호", example = "Password123!")
    private String password;

}
