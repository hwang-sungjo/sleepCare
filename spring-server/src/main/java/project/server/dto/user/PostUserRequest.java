package project.server.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostUserRequest {

    @NotBlank(message = "password: {NotBlank}")
    private String password;

    @NotBlank(message = "nickname: {NotBlank}")
    private String nickname;

    public void resetPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

}