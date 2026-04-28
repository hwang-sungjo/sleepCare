package project.server.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "회원가입 요청 DTO")
public class PostUserRequest {

    @NotBlank(message = "nickname: {NotBlank}")
    @Schema(description = "서비스 표시/로그인에 사용하는 닉네임", example = "sleepy_user")
    private String nickname;

    @NotBlank(message = "password: {NotBlank}")
    @Schema(description = "회원가입 비밀번호(서버에서 암호화 저장)", example = "Password123!")
    private String password;

    @Schema(hidden = true)
    public void resetPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

}