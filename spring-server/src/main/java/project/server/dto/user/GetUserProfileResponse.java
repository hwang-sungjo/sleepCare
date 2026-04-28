package project.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GetUserProfileResponse {

    private String email;
    private String nickname;
    private String phoneNumber;
    private String profileImage;
    private String status;
}
