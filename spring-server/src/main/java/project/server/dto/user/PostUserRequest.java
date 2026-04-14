package project.server.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
public class PostUserRequest {

    @Schema(description = "사용자 이메일", example = "user@example.com")
    @Email(message = "email: 이메일 형식이어야 합니다")
    @NotBlank(message = "email: {NotBlank}")
    @Length(max = 50, message = "email: 최대 {max}자리까지 가능합니다")
    private String email;

    @Schema(description = "비밀번호 (영대소문자, 특수문자 포함 8~20자)", example = "Password123@")
    @NotBlank(message = "password: {NotBlank}")
    @Length(min = 8, max = 20, message = "password: 최소 {min}자리 ~ 최대 {max}자리까지 가능합니다")
    @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}", message = "password: 대문자, 소문자, 특수문자가 적어도 하나씩은 있어야 합니다")
    private String password;

    @Schema(description = "전화번호", example = "010-1234-5678")
    @NotBlank(message = "phoneNumber: {NotBlank}")
    @Length(max = 20, message = "phoneNumber: 최대 {max}자리까지 가능합니다")
    private String phoneNumber;

    @Schema(description = "닉네임", example = "슬립케어")
    @Nullable
    @Length(max = 25, message = "nickname: 최대 {max}자리까지 가능합니다")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    @Nullable
    private String profileImage;

    public void resetPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

}