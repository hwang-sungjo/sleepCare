package project.server.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "로그인 응답 DTO(레거시/호환용)")
public class PostLoginResponse {

    @Schema(description = "로그인 사용자 식별자", example = "1")
    private long userId;

    @Schema(description = "발급된 JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String jwt;

}