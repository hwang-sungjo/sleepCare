package project.server.ai.chatbot.skill;

/**
 * 등록된 read-only 챗봇 DB 스킬 식별자. OpenAPI {@code operationId} 와 동일.
 */
public enum ChatbotSkillId {
    GET_DAILY_SLEEP_SUMMARY("get_daily_sleep_summary"),
    GET_SLEEP_TREND_ANALYSIS("get_sleep_trend_analysis"),
    GET_SLEEP_EFFICIENCY_RANKING("get_sleep_efficiency_ranking"),
    MATCH_ENVIRONMENT_WITH_SLEEP_STAGES("match_environment_with_sleep_stages"),
    ANALYZE_LIGHT_SENSITIVITY("analyze_light_sensitivity"),
    GET_CARDIOVASCULAR_METRICS("get_cardiovascular_metrics"),
    CHECK_RESPIRATORY_HEALTH("check_respiratory_health"),
    TRACK_SKIN_TEMPERATURE("track_skin_temperature"),
    EVALUATE_ADAPTIVE_ALARM_PERFORMANCE("evaluate_adaptive_alarm_performance"),
    ASSESS_SLEEP_REGULARITY("assess_sleep_regularity");

    private final String operationId;

    ChatbotSkillId(String operationId) {
        this.operationId = operationId;
    }

    public String operationId() {
        return operationId;
    }

    public static ChatbotSkillId fromOperationId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("skillId is required");
        }
        String normalized = id.trim();
        for (ChatbotSkillId skill : values()) {
            if (skill.operationId.equals(normalized)) {
                return skill;
            }
        }
        throw new IllegalArgumentException("Unknown chatbot skill: " + id);
    }
}
