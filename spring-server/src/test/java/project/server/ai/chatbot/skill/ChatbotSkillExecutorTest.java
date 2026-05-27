package project.server.ai.chatbot.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@SpringBootTest
@ActiveProfiles({"local-h2", "devPort", "secret", "web-mvc", "local-h2-db"})
@TestPropertySource(
        properties = {
            "JWT_SECRET_KEY=test-secret-key-must-be-long-enough-32chars",
            "JWT_EXPIRED_IN=3600000",
            "AI_CHATBOT_USE_TOOLS=false",
            "AI_S3_BUCKET=dummy",
            "AI_BEDROCK_KB_ID=dummy",
            "AI_BEDROCK_MODEL_ARN=arn:aws:bedrock:ap-northeast-2::foundation-model/anthropic.claude-3-haiku-20240307-v1:0",
            "AWS_REGION=ap-northeast-2"
        })
class ChatbotSkillExecutorTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    private ChatbotSkillExecutor skillExecutor;

    @Test
    void rejectsUnknownSkillId() {
        assertThatThrownBy(() -> ChatbotSkillId.fromOperationId("not_a_skill"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getDailySleepSummaryRunsWithoutError() {
        ChatbotSkillResult result = skillExecutor.execute(
                ChatbotSkillId.GET_DAILY_SLEEP_SUMMARY,
                1L,
                Map.of("record_date", LocalDate.now(KST).toString()));
        assertThat(result.skillId()).isEqualTo("get_daily_sleep_summary");
        assertThat(result.rows()).isNotNull();
    }

    @Test
    void getSleepTrendAnalysisRequiresDays7Or30() {
        assertThatThrownBy(() -> skillExecutor.execute(
                        ChatbotSkillId.GET_SLEEP_TREND_ANALYSIS,
                        1L,
                        Map.of("days", 14)))
                .isInstanceOf(IllegalArgumentException.class);

        ChatbotSkillResult result = skillExecutor.execute(
                ChatbotSkillId.GET_SLEEP_TREND_ANALYSIS, 1L, Map.of("days", 7));
        assertThat(result.meta()).containsKey("days");
    }

    @Test
    void allRegisteredSkillsExecuteWithoutError() {
        String date = LocalDate.now(KST).toString();
        for (ChatbotSkillId skill : ChatbotSkillId.values()) {
            Map<String, Object> params = switch (skill) {
                case GET_SLEEP_TREND_ANALYSIS, TRACK_SKIN_TEMPERATURE, ASSESS_SLEEP_REGULARITY ->
                        Map.of("days", 7);
                case EVALUATE_ADAPTIVE_ALARM_PERFORMANCE -> Map.of();
                default -> Map.of("record_date", date);
            };
            ChatbotSkillResult result = skillExecutor.execute(skill, 1L, params);
            assertThat(result.skillId()).isEqualTo(skill.operationId());
            assertThat(result.rows()).isNotNull();
        }
    }
}
