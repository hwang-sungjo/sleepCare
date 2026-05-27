package project.server.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Bedrock Knowledge Base RAG 인용 1건 — 챗봇·대시보드 {@code aiAdvice} 공용.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Bedrock RAG 인용 1건")
public class CitationItem {

    @Schema(
            description = "출처 S3 URI 등 위치 문자열",
            example = "s3://sleepcare-knowledge-base-2026/papers/2024-deep-sleep.pdf")
    private String location;

    @Schema(description = "인용된 청크 텍스트(잘릴 수 있음)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String snippet;
}
