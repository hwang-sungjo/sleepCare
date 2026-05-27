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
 * 챗봇: {@code app.ai.chatbot.use-tools=true} 이면 Bedrock Converse + DB 스킬,
 * false 이면 기존 Knowledge Base RAG.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final S3PromptService s3PromptService;
    private final BedrockKnowledgeBaseService bedrockKnowledgeBaseService;
    private final BedrockConverseService bedrockConverseService;

    @Value("${app.ai.prompt-key-chatbot}")
    private String promptKey;

    @Value("${app.ai.chatbot.use-tools:true}")
    private boolean useTools;

    public ChatResponse reply(long userId, ChatRequest request) {
        String systemPrompt;
        try {
            systemPrompt = s3PromptService.getPrompt(promptKey);
        } catch (RuntimeException e) {
            log.warn("[ChatbotService] prompt load failed userId={}: {}", userId, e.getMessage());
            throw new AiException(AI_PROMPT_LOAD_FAILED, e);
        }

        if (useTools) {
            return replyWithTools(userId, request, systemPrompt);
        }
        return replyWithKnowledgeBase(userId, request, systemPrompt);
    }

    private ChatResponse replyWithTools(long userId, ChatRequest request, String systemPrompt) {
        try {
            BedrockConverseService.ConverseChatResult result = bedrockConverseService.chat(
                    userId, request.getMessage(), systemPrompt, request.getSessionId());
            return ChatResponse.builder()
                    .reply(result.reply())
                    .sessionId(result.sessionId())
                    .toolCalls(result.toolCalls())
                    .build();
        } catch (RuntimeException e) {
            log.warn("[ChatbotService] converse failed userId={}: {}", userId, e.getMessage());
            throw new AiException(AI_GENERATION_FAILED, e);
        }
    }

    private ChatResponse replyWithKnowledgeBase(long userId, ChatRequest request, String systemPrompt) {
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
