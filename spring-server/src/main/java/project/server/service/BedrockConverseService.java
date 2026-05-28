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

    public ConverseChatResult chat(long userId, String userMessage, String systemPrompt, String sessionId) {
        if (converseModelArn == null || converseModelArn.isBlank()) {
            throw new IllegalStateException("app.ai.chatbot.converse-model-arn (or app.ai.bedrock.model-arn) is not configured");
        }

        String resolvedSession = conversationStore.resolveSessionId(sessionId);
        List<Message> history = conversationStore.history(resolvedSession);
        history.add(userMessage(userMessage));

        String effectiveSystemPrompt = augmentSystemPromptWithKstToday(systemPrompt);

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
     * 모델이 record_date 등을 임의 연도(학습 데이터)로 채우지 않도록 KST 오늘 날짜를 system 에 붙인다.
     */
    static String augmentSystemPromptWithKstToday(String systemPrompt) {
        LocalDate today = LocalDate.now(KST);
        String dateBlock =
                """

                [시스템 시각] Asia/Seoul(KST) 기준 오늘 날짜: %s.
                record_date 등 날짜 파라미터는 반드시 이 날짜를 기준으로 계산한다 (어제 = 오늘에서 1일 전).
                학습 데이터나 임의의 과거 연도 날짜를 사용하지 않는다.
                """
                        .formatted(today);
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return dateBlock.stripLeading();
        }
        return systemPrompt + dateBlock;
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
