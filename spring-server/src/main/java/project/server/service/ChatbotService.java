package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import project.server.common.exception.AiException;
import project.server.dto.ai.ChatRequest;
import project.server.dto.ai.ChatResponse;
import project.server.dto.ai.CitationItem;

import java.util.List;

import static project.server.common.response.status.BaseExceptionResponseStatus.AI_GENERATION_FAILED;
import static project.server.common.response.status.BaseExceptionResponseStatus.AI_PROMPT_LOAD_FAILED;

/**
 * 챗봇: KB 논문 검색(Retrieve) + Bedrock Converse + DB 스킬.
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

    public ChatResponse reply(long userId, ChatRequest request) {
        String systemPrompt;
        try {
            systemPrompt = s3PromptService.getPrompt(promptKey);
        } catch (RuntimeException e) {
            log.warn("[ChatbotService] prompt load failed userId={}: {}", userId, e.getMessage());
            throw new AiException(AI_PROMPT_LOAD_FAILED, e);
        }

        List<CitationItem> citations = null;
        String kbContext = "";
        try {
            KnowledgeBaseRetrieveResult kb = bedrockKnowledgeBaseService.retrieveContext(request.getMessage());
            citations = kb.citations();
            kbContext = kb.contextForPrompt();
        } catch (RuntimeException e) {
            log.warn("[ChatbotService] KB retrieve failed userId={}: {}", userId, e.getMessage());
        }

        try {
            BedrockConverseService.ConverseChatResult result = bedrockConverseService.chat(
                    userId, request.getMessage(), systemPrompt, kbContext, request.getSessionId());
            return ChatResponse.builder()
                    .reply(result.reply())
                    .sessionId(result.sessionId())
                    .citations(citations)
                    .toolCalls(result.toolCalls())
                    .build();
        } catch (RuntimeException e) {
            log.warn("[ChatbotService] converse failed userId={}: {}", userId, e.getMessage());
            throw new AiException(AI_GENERATION_FAILED, e);
        }
    }
}
