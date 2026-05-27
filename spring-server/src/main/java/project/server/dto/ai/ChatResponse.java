package project.server.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "AI 챗봇 응답")
public class ChatResponse {

    @Schema(description = "Bedrock 가 생성한 한국어 답변 본문")
    private String reply;

    @Schema(
            description = "이번 응답의 세션 식별자. 다음 요청에 그대로 실어 보내면 멀티턴이 됩니다.",
            example = "9c1f0c1a-aa57-4ce4-a8cf-1b9d5b6a59e2")
    private String sessionId;

    @Schema(description = "답변 근거로 사용된 KB 청크의 출처 정보 (없을 수 있음)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CitationItem> citations;
}
