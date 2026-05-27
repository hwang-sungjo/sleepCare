package project.server.ai.chatbot.skill;

import java.util.List;
import java.util.Map;

/**
 * 스킬 실행 결과 — Bedrock toolResult JSON 직렬화용.
 */
public record ChatbotSkillResult(
        String skillId,
        List<Map<String, Object>> rows,
        Map<String, Object> meta) {

    public static ChatbotSkillResult of(String skillId, List<Map<String, Object>> rows) {
        return new ChatbotSkillResult(skillId, rows, Map.of());
    }

    public static ChatbotSkillResult of(String skillId, List<Map<String, Object>> rows, Map<String, Object> meta) {
        return new ChatbotSkillResult(skillId, rows, meta == null ? Map.of() : meta);
    }
}
