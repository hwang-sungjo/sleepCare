package project.server.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "로그인 응답 DTO")
public class LoginResponse {

    @Schema(description = "서비스 내부 사용자 식별자", example = "1")
    private long userId;

    @Schema(description = "인증/인가에 사용하는 JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String jwt;

}
