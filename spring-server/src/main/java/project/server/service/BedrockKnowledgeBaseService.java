package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseRetrieveAndGenerateConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateType;

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

    private static String composeInput(String systemPrompt, String userText) {
        String safePrompt = systemPrompt == null ? "" : systemPrompt;
        String safeUser = userText == null ? "" : userText;
        if (safePrompt.isBlank()) {
            return safeUser;
        }
        return safePrompt + "\n\n" + safeUser;
    }
}
