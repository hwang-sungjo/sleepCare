package project.server.ai.chatbot.skill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 등록된 read-only SQL 스킬만 실행한다. {@code userId} 는 JWT 에서만 주입한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotSkillExecutor {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ConcurrentHashMap<String, String> sqlCache = new ConcurrentHashMap<>();

    public String executeToJson(ChatbotSkillId skillId, long userId, Map<String, Object> toolInput) {
        try {
            ChatbotSkillResult result = execute(skillId, userId, toolInput);
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize skill result", e);
        }
    }

    public ChatbotSkillResult execute(ChatbotSkillId skillId, long userId, Map<String, Object> toolInput) {
        Map<String, Object> input = toolInput == null ? Map.of() : toolInput;
        log.info("[ChatbotSkillExecutor] skill={} userId={} params={}", skillId.operationId(), userId, input);

        return switch (skillId) {
            case GET_DAILY_SLEEP_SUMMARY -> getDailySleepSummary(userId, input);
            case GET_SLEEP_TREND_ANALYSIS -> getSleepTrendAnalysis(userId, input);
            case GET_SLEEP_EFFICIENCY_RANKING -> getSleepEfficiencyRanking(userId, input);
            case MATCH_ENVIRONMENT_WITH_SLEEP_STAGES -> matchEnvironmentWithSleepStages(userId, input);
            case ANALYZE_LIGHT_SENSITIVITY -> analyzeLightSensitivity(userId, input);
            case GET_CARDIOVASCULAR_METRICS -> getCardiovascularMetrics(userId, input);
            case CHECK_RESPIRATORY_HEALTH -> checkRespiratoryHealth(userId, input);
            case TRACK_SKIN_TEMPERATURE -> trackSkinTemperature(userId, input);
            case EVALUATE_ADAPTIVE_ALARM_PERFORMANCE -> evaluateAdaptiveAlarmPerformance(userId, input);
            case ASSESS_SLEEP_REGULARITY -> assessSleepRegularity(userId, input);
        };
    }

    private ChatbotSkillResult getDailySleepSummary(long userId, Map<String, Object> input) {
        LocalDate recordDate = requireRecordDate(input);
        var params = baseParams(userId).addValue("recordDate", recordDate);
        List<Map<String, Object>> rows = query("sql/chatbot/get_daily_sleep_summary.sql", params);
        return ChatbotSkillResult.of(ChatbotSkillId.GET_DAILY_SLEEP_SUMMARY.operationId(), rows);
    }

    private ChatbotSkillResult getSleepTrendAnalysis(long userId, Map<String, Object> input) {
        int days = requireDays(input);
        LocalDate endDate = LocalDate.now(KST);
        LocalDate startDate = endDate.minusDays(days - 1L);
        var params = baseParams(userId).addValue("startDate", startDate).addValue("endDate", endDate);
        List<Map<String, Object>> rows = query("sql/chatbot/get_sleep_trend_analysis.sql", params);
        return ChatbotSkillResult.of(
                ChatbotSkillId.GET_SLEEP_TREND_ANALYSIS.operationId(),
                rows,
                Map.of("days", days, "startDate", startDate.toString(), "endDate", endDate.toString()));
    }

    private ChatbotSkillResult getSleepEfficiencyRanking(long userId, Map<String, Object> input) {
        LocalDate recordDate = requireRecordDate(input);
        var params = baseParams(userId).addValue("recordDate", recordDate);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(query("sql/chatbot/get_sleep_efficiency_ranking.sql", params));
        rows.addAll(query("sql/chatbot/get_sleep_efficiency_ranking_best.sql", params));
        rows.addAll(query("sql/chatbot/get_sleep_efficiency_ranking_stats.sql", params));
        return ChatbotSkillResult.of(ChatbotSkillId.GET_SLEEP_EFFICIENCY_RANKING.operationId(), rows);
    }

    private ChatbotSkillResult matchEnvironmentWithSleepStages(long userId, Map<String, Object> input) {
        LocalDate recordDate = requireRecordDate(input);
        var params = baseParams(userId).addValue("recordDate", recordDate);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(query("sql/chatbot/match_environment_with_sleep_stages_summary.sql", params));
        rows.addAll(query("sql/chatbot/match_environment_with_sleep_stages.sql", params));
        return ChatbotSkillResult.of(ChatbotSkillId.MATCH_ENVIRONMENT_WITH_SLEEP_STAGES.operationId(), rows);
    }

    private ChatbotSkillResult analyzeLightSensitivity(long userId, Map<String, Object> input) {
        LocalDate recordDate = requireRecordDate(input);
        var params = baseParams(userId).addValue("recordDate", recordDate);
        int dayOfWeek = recordDate.getDayOfWeek().getValue();
        params.addValue("dayOfWeek", dayOfWeek);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(query("sql/chatbot/analyze_light_sensitivity_env.sql", params));
        rows.addAll(query("sql/chatbot/analyze_light_sensitivity_alarm.sql", params));
        return ChatbotSkillResult.of(
                ChatbotSkillId.ANALYZE_LIGHT_SENSITIVITY.operationId(),
                rows,
                Map.of("recordDate", recordDate.toString(), "dayOfWeek", dayOfWeek));
    }

    private ChatbotSkillResult getCardiovascularMetrics(long userId, Map<String, Object> input) {
        LocalDate recordDate = requireRecordDate(input);
        var params = baseParams(userId).addValue("recordDate", recordDate);
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> hr = singleRow(query("sql/chatbot/get_cardiovascular_metrics_hr.sql", params));
        Map<String, Object> hrv = singleRow(query("sql/chatbot/get_cardiovascular_metrics_hrv.sql", params));
        Map<String, Object> combined = new LinkedHashMap<>();
        combined.put("metric_group", "heart_rate");
        combined.putAll(hr);
        rows.add(combined);
        Map<String, Object> combinedHrv = new LinkedHashMap<>();
        combinedHrv.put("metric_group", "hrv");
        combinedHrv.putAll(hrv);
        rows.add(combinedHrv);
        return ChatbotSkillResult.of(ChatbotSkillId.GET_CARDIOVASCULAR_METRICS.operationId(), rows);
    }

    private ChatbotSkillResult checkRespiratoryHealth(long userId, Map<String, Object> input) {
        LocalDate recordDate = requireRecordDate(input);
        var params = baseParams(userId).addValue("recordDate", recordDate);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(query("sql/chatbot/check_respiratory_health_summary.sql", params));
        Map<String, Object> spo2 = singleRow(query("sql/chatbot/check_respiratory_health_spo2.sql", params));
        spo2.put("metric_group", "spo2");
        rows.add(spo2);
        return ChatbotSkillResult.of(ChatbotSkillId.CHECK_RESPIRATORY_HEALTH.operationId(), rows);
    }

    private ChatbotSkillResult trackSkinTemperature(long userId, Map<String, Object> input) {
        int days = requireDays(input);
        LocalDate endDate = LocalDate.now(KST);
        LocalDate startDate = endDate.minusDays(days - 1L);
        var params = baseParams(userId).addValue("startDate", startDate).addValue("endDate", endDate);
        List<Map<String, Object>> rows = query("sql/chatbot/track_skin_temperature.sql", params);
        return ChatbotSkillResult.of(
                ChatbotSkillId.TRACK_SKIN_TEMPERATURE.operationId(),
                rows,
                Map.of("days", days, "startDate", startDate.toString(), "endDate", endDate.toString()));
    }

    private ChatbotSkillResult evaluateAdaptiveAlarmPerformance(long userId, Map<String, Object> input) {
        int days = optionalDays(input, 7);
        List<Map<String, Object>> rows = query("sql/chatbot/evaluate_adaptive_alarm_performance.sql", baseParams(userId));
        return ChatbotSkillResult.of(
                ChatbotSkillId.EVALUATE_ADAPTIVE_ALARM_PERFORMANCE.operationId(),
                rows,
                Map.of("lookbackDaysHint", days));
    }

    private ChatbotSkillResult assessSleepRegularity(long userId, Map<String, Object> input) {
        int days = requireDays(input);
        LocalDate endDate = LocalDate.now(KST);
        LocalDate startDate = endDate.minusDays(days - 1L);
        var params = baseParams(userId).addValue("startDate", startDate).addValue("endDate", endDate);
        List<Map<String, Object>> rows = query("sql/chatbot/assess_sleep_regularity.sql", params);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("days", days);
        meta.put("startDate", startDate.toString());
        meta.put("endDate", endDate.toString());
        meta.put("regularityHint", computeRegularityHint(rows));
        return ChatbotSkillResult.of(ChatbotSkillId.ASSESS_SLEEP_REGULARITY.operationId(), rows, meta);
    }

    private static String computeRegularityHint(List<Map<String, Object>> rows) {
        if (rows.size() < 2) {
            return "insufficient_data";
        }
        return "compare_start_end_times_across_rows";
    }

    private List<Map<String, Object>> query(String classpathSql, MapSqlParameterSource params) {
        String sql = loadSql(classpathSql);
        assertSelectOnly(sql);
        return jdbc.queryForList(sql, params);
    }

    private static Map<String, Object> singleRow(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private static MapSqlParameterSource baseParams(long userId) {
        return new MapSqlParameterSource("userId", userId);
    }

    private String loadSql(String classpathLocation) {
        return sqlCache.computeIfAbsent(classpathLocation, loc -> {
            try {
                ClassPathResource resource = new ClassPathResource(loc);
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Missing SQL resource: " + loc, e);
            }
        });
    }

    private static void assertSelectOnly(String sql) {
        String normalized = sql.strip().toUpperCase();
        if (!normalized.startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT statements are allowed for chatbot skills");
        }
        if (normalized.contains(";") && normalized.indexOf(';') < normalized.length() - 1) {
            throw new IllegalArgumentException("Multiple SQL statements are not allowed");
        }
    }

    private static LocalDate requireRecordDate(Map<String, Object> input) {
        Object raw = input.get("record_date");
        if (raw == null) {
            raw = input.get("recordDate");
        }
        if (raw == null) {
            throw new IllegalArgumentException("record_date is required (YYYY-MM-DD)");
        }
        try {
            return LocalDate.parse(raw.toString().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid record_date: " + raw, e);
        }
    }

    private static int requireDays(Map<String, Object> input) {
        return normalizeDays(input.get("days"), true);
    }

    private static int optionalDays(Map<String, Object> input, int defaultValue) {
        Object raw = input.get("days");
        if (raw == null) {
            return defaultValue;
        }
        return normalizeDays(raw, false);
    }

    private static int normalizeDays(Object raw, boolean required) {
        if (raw == null) {
            if (required) {
                throw new IllegalArgumentException("days is required (7 or 30)");
            }
            return 7;
        }
        int days;
        if (raw instanceof Number n) {
            days = n.intValue();
        } else {
            days = Integer.parseInt(raw.toString().trim());
        }
        if (days != 7 && days != 30) {
            throw new IllegalArgumentException("days must be 7 or 30");
        }
        return days;
    }
}
