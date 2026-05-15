package project.server.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "회원가입 응답 DTO")
public class PostUserResponse {

    @Schema(description = "새로 생성된 사용자 식별자", example = "1")
    private long userId;

    @Schema(description = "회원가입 직후 발급되는 JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String jwt;

}