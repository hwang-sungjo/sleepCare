package project.server.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 챗봇 단일 메시지 요청")
public class ChatRequest {

    @Schema(description = "사용자가 챗봇에게 보내는 메시지 (한국어).", example = "어젯밤에 잠을 잘 못 잤어, 왜 그런지 알 수 있을까?")
    @NotBlank
    private String message;

    @Schema(
            description = "이전 응답에서 받은 Bedrock 세션 식별자. 첫 메시지일 경우 비워서 보냅니다.",
            example = "",
            nullable = true)
    private String sessionId;
}
