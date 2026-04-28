package project.server.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "내 프로필 조회 응답 DTO")
public class GetUserProfileResponse {

    @Schema(description = "현재 로그인 사용자의 닉네임", example = "sleepy_user")
    private String nickname;
}
