package project.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import project.server.ai.chatbot.ChatbotConversationStore;
import project.server.ai.chatbot.skill.ChatbotSkillExecutor;
import project.server.ai.chatbot.skill.ChatbotSkillId;
import project.server.ai.chatbot.skill.ChatbotToolSpecFactory;
import project.server.dto.ai.ChatResponse;

import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bedrock Converse API + DB 스킬 tool-use 루프.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockConverseService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ChatbotSkillExecutor skillExecutor;
    private final ChatbotConversationStore conversationStore;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.chatbot.converse-model-arn:${app.ai.bedrock.model-arn}}")
    private String converseModelArn;

    @Value("${app.ai.chatbot.max-tool-rounds:5}")
    private int maxToolRounds;

    public record ConverseChatResult(String reply, String sessionId, List<ChatResponse.ToolCallItem> toolCalls) {
    }

    public ConverseChatResult chat(
            long userId, String userMessage, String systemPrompt, String knowledgeBaseContext, String sessionId) {
        if (converseModelArn == null || converseModelArn.isBlank()) {
            throw new IllegalStateException("app.ai.chatbot.converse-model-arn (or app.ai.bedrock.model-arn) is not configured");
        }

        String resolvedSession = conversationStore.resolveSessionId(sessionId);
        List<Message> history = conversationStore.history(resolvedSession);
        history.add(userMessage(userMessage));

        String effectiveSystemPrompt = augmentSystemPromptWithKstToday(systemPrompt, knowledgeBaseContext);

        List<ChatResponse.ToolCallItem> toolCalls = new ArrayList<>();
        ToolConfiguration toolConfig = ToolConfiguration.builder()
                .tools(ChatbotToolSpecFactory.allTools())
                .build();

        String replyText = null;
        for (int round = 0; round <= maxToolRounds; round++) {
            ConverseResponse response = invoke(effectiveSystemPrompt, history, toolConfig);
            Message assistantMessage = response.output().message();
            history.add(assistantMessage);

            if (response.stopReason() != StopReason.TOOL_USE) {
                replyText = extractText(assistantMessage);
                break;
            }

            List<ToolUseBlock> toolUses = extractToolUses(assistantMessage);
            if (toolUses.isEmpty()) {
                replyText = extractText(assistantMessage);
                break;
            }

            List<ContentBlock> toolResultBlocks = new ArrayList<>();
            for (ToolUseBlock toolUse : toolUses) {
                String skillName = toolUse.name();
                Map<String, Object> input = documentToMap(toolUse.input());
                String resultJson;
                try {
                    ChatbotSkillId skillId = ChatbotSkillId.fromOperationId(skillName);
                    resultJson = skillExecutor.executeToJson(skillId, userId, input);
                    toolCalls.add(ChatResponse.ToolCallItem.builder()
                            .skillId(skillName)
                            .status("ok")
                            .build());
                } catch (Exception e) {
                    log.warn("[BedrockConverseService] skill failed skill={} userId={}: {}",
                            skillName, userId, e.getMessage());
                    resultJson = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                    toolCalls.add(ChatResponse.ToolCallItem.builder()
                            .skillId(skillName)
                            .status("error")
                            .build());
                }
                toolResultBlocks.add(ContentBlock.builder()
                        .toolResult(ToolResultBlock.builder()
                                .toolUseId(toolUse.toolUseId())
                                .content(ToolResultContentBlock.builder().text(resultJson).build())
                                .build())
                        .build());
            }
            history.add(Message.builder()
                    .role(ConversationRole.USER)
                    .content(toolResultBlocks)
                    .build());
        }

        if (replyText == null || replyText.isBlank()) {
            replyText = "요청을 처리했지만 최종 답변을 생성하지 못했습니다. 다시 질문해 주세요.";
        }
        return new ConverseChatResult(replyText.trim(), resolvedSession, toolCalls.isEmpty() ? null : toolCalls);
    }

    /**
     * KST 오늘 날짜·KB 검색 청크를 system prompt 에 붙인다.
     */
    static String augmentSystemPromptWithKstToday(String systemPrompt, String knowledgeBaseContext) {
        LocalDate today = LocalDate.now(KST);
        String dateBlock =
                """

                [시스템 시각] Asia/Seoul(KST) 기준 오늘 날짜: %s.

                [record_date 규칙] daily_health_summary 의 record_date 는 기상일(일어난 날, KST)이다.
                - 사용자가 "어제 수면", "어젯밤 수면", "지난밤 수면", "오늘 아침에 일어난 수면" 등을 말하면 record_date = 오늘(%s).
                  (어제 밤에 잠들어 오늘 아침에 기상했다고 가정한다. 달력상 어제 날짜를 쓰지 않는다.)
                - "그저께 밤", "이틀 전 밤" 등은 기상일을 계산해 record_date 에 넣는다.
                  예: 그저께 밤 수면(어제 아침 기상) → record_date = 오늘에서 1일 전.
                - 학습 데이터나 임의의 과거 연도 날짜를 사용하지 않는다.
                """
                        .formatted(today, today);
        String merged = systemPrompt == null || systemPrompt.isBlank()
                ? dateBlock.stripLeading()
                : systemPrompt + dateBlock;
        return appendKnowledgeBaseContext(merged, knowledgeBaseContext);
    }

    static String appendKnowledgeBaseContext(String systemPrompt, String knowledgeBaseContext) {
        if (knowledgeBaseContext == null || knowledgeBaseContext.isBlank()) {
            return systemPrompt;
        }
        return systemPrompt
                + """

                [Knowledge Base — 수면 관련 논문/가이드 발췌]
                개인 수치·알람·일별 기록은 반드시 DB tool 로 조회한다. 아래 발췌는 일반 근거·설명용이다.
                """
                + knowledgeBaseContext;
    }

    private ConverseResponse invoke(String systemPrompt, List<Message> messages, ToolConfiguration toolConfig) {
        ConverseRequest.Builder builder = ConverseRequest.builder()
                .modelId(converseModelArn)
                .messages(messages)
                .toolConfig(toolConfig);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.system(SystemContentBlock.builder().text(systemPrompt).build());
        }
        return bedrockRuntimeClient.converse(builder.build());
    }

    private static Message userMessage(String text) {
        return Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(text))
                .build();
    }

    private static String extractText(Message message) {
        if (message == null || message.content() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : message.content()) {
            if (block.text() != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(block.text());
            }
        }
        return sb.toString();
    }

    private static List<ToolUseBlock> extractToolUses(Message message) {
        List<ToolUseBlock> uses = new ArrayList<>();
        if (message == null || message.content() == null) {
            return uses;
        }
        for (ContentBlock block : message.content()) {
            if (block.toolUse() != null) {
                uses.add(block.toolUse());
            }
        }
        return uses;
    }

    private Map<String, Object> documentToMap(Document document) {
        if (document == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(document.toString(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[BedrockConverseService] failed to parse tool input: {}", e.getMessage());
            return Map.of();
        }
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
