package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import project.server.common.exception.AiException;
import project.server.dto.ai.ChatRequest;
import project.server.dto.ai.ChatResponse;

import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateResponse;

import static project.server.common.response.status.BaseExceptionResponseStatus.AI_GENERATION_FAILED;
import static project.server.common.response.status.BaseExceptionResponseStatus.AI_PROMPT_LOAD_FAILED;

/**
 * 페르소나 {@code skill_chatbot.md} 와 클라이언트가 보낸 메시지를 Bedrock RAG 에 그대로 위임한다.
 *
 * <p>
 * 서버는 사용자 메트릭을 자동 결합하지 않으며, 멀티턴은 클라이언트가 sessionId 를 들고 다닌다.
 * Bedrock/S3 호출 실패는 {@link AiException} 으로 매핑되어 사용자에게 명확한 503 응답이 간다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final S3PromptService s3PromptService;
    private final BedrockKnowledgeBaseService bedrockKnowledgeBaseService;

    @Value("${app.ai.prompt-key-chatbot}")
    private String promptKey;

    public ChatResponse reply(long userId, ChatRequest request) {
        String systemPrompt;
        try {
            systemPrompt = s3PromptService.getPrompt(promptKey);
        } catch (RuntimeException e) {
            log.warn("[ChatbotService] prompt load failed userId={}: {}", userId, e.getMessage());
            throw new AiException(AI_PROMPT_LOAD_FAILED, e);
        }

        RetrieveAndGenerateResponse response;
        try {
            response = bedrockKnowledgeBaseService.retrieveAndGenerate(
                    request.getMessage(), systemPrompt, request.getSessionId());
        } catch (RuntimeException e) {
            log.warn("[ChatbotService] bedrock invocation failed userId={}: {}", userId, e.getMessage());
            throw new AiException(AI_GENERATION_FAILED, e);
        }

        String text = response.output() == null ? null : response.output().text();
        if (text == null || text.isBlank()) {
            log.warn("[ChatbotService] empty bedrock response userId={}", userId);
            throw new AiException(AI_GENERATION_FAILED);
        }

        return ChatResponse.builder()
                .reply(text.trim())
                .sessionId(response.sessionId())
                .citations(BedrockCitationMapper.fromBedrock(response.citations()))
                .build();
    }
}
