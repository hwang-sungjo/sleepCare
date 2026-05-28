package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import project.server.dto.ai.CitationItem;

import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseQuery;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseRetrievalResult;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseRetrieveAndGenerateConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateType;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveResponse;

import java.util.List;

/**
 * Bedrock Knowledge Base RAG (RetrieveAndGenerate) 호출 래퍼.
 *
 * <p>
 * KB-RAG API 에는 별도의 system prompt 슬롯이 없으므로
 * {@code systemPrompt + "\n\n" + userText} 를 한 번에 입력으로 보낸다.
 * 호출자는 {@link #retrieveAndGenerate(String, String, String)} 의 sessionId 를
 * null 로 주면 첫 턴, 이전 응답의 ID 를 주면 멀티턴이 된다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockKnowledgeBaseService {

    private static final int MAX_RETRIEVAL_RESULTS = 5;
    private static final int MAX_SNIPPET_CHARS = 900;

    private final BedrockAgentRuntimeClient bedrockClient;

    @Value("${app.ai.bedrock.knowledge-base-id}")
    private String knowledgeBaseId;

    @Value("${app.ai.bedrock.model-arn}")
    private String modelArn;

    /**
     * KB 검색 결과를 근거로 답변을 생성한다.
     *
     * @param userText     사용자(혹은 서버가 구성한) 입력 텍스트
     * @param systemPrompt S3 에서 읽어온 페르소나 프롬프트 (앞쪽에 합쳐 보냄)
     * @param sessionId    멀티턴 식별자; null/blank 면 첫 턴
     * @return Bedrock RAG 응답 (output.text / sessionId / citations 포함)
     */
    public RetrieveAndGenerateResponse retrieveAndGenerate(String userText, String systemPrompt, String sessionId) {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            throw new IllegalStateException("app.ai.bedrock.knowledge-base-id is not configured");
        }
        if (modelArn == null || modelArn.isBlank()) {
            throw new IllegalStateException("app.ai.bedrock.model-arn is not configured");
        }

        String composed = composeInput(systemPrompt, userText);

        RetrieveAndGenerateRequest.Builder builder = RetrieveAndGenerateRequest.builder()
                .input(RetrieveAndGenerateInput.builder().text(composed).build())
                .retrieveAndGenerateConfiguration(RetrieveAndGenerateConfiguration.builder()
                        .type(RetrieveAndGenerateType.KNOWLEDGE_BASE)
                        .knowledgeBaseConfiguration(KnowledgeBaseRetrieveAndGenerateConfiguration.builder()
                                .knowledgeBaseId(knowledgeBaseId)
                                .modelArn(modelArn)
                                .build())
                        .build());

        if (sessionId != null && !sessionId.isBlank()) {
            builder.sessionId(sessionId);
        }

        log.debug("[BedrockKnowledgeBaseService] invoke kb={} sessionId={} inputLen={}",
                knowledgeBaseId, sessionId, composed.length());
        return bedrockClient.retrieveAndGenerate(builder.build());
    }

    /**
     * KB 에서 논문 청크만 검색한다 (생성 없음). Converse+스킬 경로에서 컨텍스트·citations 용.
     */
    public KnowledgeBaseRetrieveResult retrieveContext(String query) {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            log.warn("[BedrockKnowledgeBaseService] knowledge-base-id not configured; skip retrieve");
            return KnowledgeBaseRetrieveResult.empty();
        }
        if (query == null || query.isBlank()) {
            return KnowledgeBaseRetrieveResult.empty();
        }

        RetrieveResponse response = bedrockClient.retrieve(RetrieveRequest.builder()
                .knowledgeBaseId(knowledgeBaseId)
                .retrievalQuery(KnowledgeBaseQuery.builder().text(query.trim()).build())
                .build());

        List<KnowledgeBaseRetrievalResult> results = response.retrievalResults();
        if (results == null || results.isEmpty()) {
            return KnowledgeBaseRetrieveResult.empty();
        }

        int limit = Math.min(results.size(), MAX_RETRIEVAL_RESULTS);
        List<KnowledgeBaseRetrievalResult> trimmed = results.subList(0, limit);
        List<CitationItem> citations = BedrockCitationMapper.fromRetrievalResults(trimmed);
        String contextForPrompt = formatContextBlock(trimmed);
        return new KnowledgeBaseRetrieveResult(citations, contextForPrompt);
    }

    private static String formatContextBlock(List<KnowledgeBaseRetrievalResult> results) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (KnowledgeBaseRetrievalResult result : results) {
            String snippet = result.content() == null ? null : result.content().text();
            if (snippet == null || snippet.isBlank()) {
                continue;
            }
            String trimmed = snippet.strip();
            if (trimmed.length() > MAX_SNIPPET_CHARS) {
                trimmed = trimmed.substring(0, MAX_SNIPPET_CHARS) + "…";
            }
            String location = retrievalLocationUri(result);
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("[").append(index++).append("] ");
            if (location != null && !location.isBlank()) {
                sb.append("출처: ").append(location).append("\n");
            }
            sb.append(trimmed);
        }
        return sb.toString();
    }

    private static String retrievalLocationUri(KnowledgeBaseRetrievalResult result) {
        if (result.location() == null || result.location().s3Location() == null) {
            return null;
        }
        return result.location().s3Location().uri();
    }

    private static String composeInput(String systemPrompt, String userText) {
        String safePrompt = systemPrompt == null ? "" : systemPrompt;
        String safeUser = userText == null ? "" : userText;
        if (safePrompt.isBlank()) {
            return safeUser;
        }
        return safePrompt + "\n\n" + safeUser;
    }
}
