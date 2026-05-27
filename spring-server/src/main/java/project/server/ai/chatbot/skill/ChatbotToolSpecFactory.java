package project.server.ai.chatbot.skill;

import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Bedrock Converse {@link Tool} 정의 — {@link docs/02-chatbot-skills-openapi.json} 과 동일 operationId.
 */
public final class ChatbotToolSpecFactory {

    private ChatbotToolSpecFactory() {
    }

    public static List<Tool> allTools() {
        return Arrays.asList(
                tool("get_daily_sleep_summary",
                        "특정 날짜의 일일 수면 요약(time_in_bed, efficiency, 수면 단계별 분)을 조회합니다.",
                        dateOnlySchema()),
                tool("get_sleep_trend_analysis",
                        "최근 7일 또는 30일간 수면 효율·수면 시간 추이를 조회합니다.",
                        daysOnlySchema()),
                tool("get_sleep_efficiency_ranking",
                        "기준일 수면 효율과 과거 최고 효율·통계를 비교합니다.",
                        dateOnlySchema()),
                tool("match_environment_with_sleep_stages",
                        "수면 구간 환경(온습도·조도)과 수면 단계를 매칭합니다.",
                        dateOnlySchema()),
                tool("analyze_light_sensitivity",
                        "수면 중 조도와 알람 기상 시각을 비교합니다.",
                        dateOnlySchema()),
                tool("get_cardiovascular_metrics",
                        "심박(BPM)과 HRV(RMSSD) 일별 통계를 조회합니다.",
                        dateOnlySchema()),
                tool("check_respiratory_health",
                        "SpO2와 호흡수(breathing_rate)를 조회합니다.",
                        dateOnlySchema()),
                tool("track_skin_temperature",
                        "skin_temp_relative 추이를 조회합니다.",
                        daysOnlySchema()),
                tool("evaluate_adaptive_alarm_performance",
                        "요일별 알람(base_wake_time, dynamic_wake_at, window) 설정을 조회합니다.",
                        daysOptionalSchema()),
                tool("assess_sleep_regularity",
                        "취침·기상 시각 일관성 분석용 일별 start_time/end_time을 조회합니다.",
                        daysOnlySchema()));
    }

    private static Tool tool(String name, String description, Document inputSchema) {
        return Tool.builder()
                .toolSpec(ToolSpecification.builder()
                        .name(name)
                        .description(description)
                        .inputSchema(ToolInputSchema.builder().json(inputSchema).build())
                        .build())
                .build();
    }

    /**
     * Bedrock Converse는 {@code inputSchema.json}에 객체 형태의 Document가 필요합니다.
     * {@link Document#fromString(String)}은 JSON 스키마 문자열을 파싱하지 않고 문자열 스칼라로 보내 400이 납니다.
     */
    private static Document dateOnlySchema() {
        Document recordDate = Document.fromMap(Map.of(
                "type", Document.fromString("string"),
                "description", Document.fromString("기준 날짜 (YYYY-MM-DD, KST 달력)")));
        return Document.fromMap(Map.of(
                "type", Document.fromString("object"),
                "properties", Document.fromMap(Map.of("record_date", recordDate)),
                "required", Document.fromList(List.of(Document.fromString("record_date")))));
    }

    private static Document daysOnlySchema() {
        Document days = Document.fromMap(Map.of(
                "type", Document.fromString("integer"),
                "enum", Document.fromList(List.of(
                        Document.fromNumber(BigDecimal.valueOf(7)),
                        Document.fromNumber(BigDecimal.valueOf(30)))),
                "description", Document.fromString("조회 기간(일)")));
        return Document.fromMap(Map.of(
                "type", Document.fromString("object"),
                "properties", Document.fromMap(Map.of("days", days)),
                "required", Document.fromList(List.of(Document.fromString("days")))));
    }

    private static Document daysOptionalSchema() {
        Document days = Document.fromMap(Map.of(
                "type", Document.fromString("integer"),
                "enum", Document.fromList(List.of(
                        Document.fromNumber(BigDecimal.valueOf(7)),
                        Document.fromNumber(BigDecimal.valueOf(30)))),
                "description", Document.fromString("참고용 lookback 힌트(선택, 기본 7)")));
        return Document.fromMap(Map.of(
                "type", Document.fromString("object"),
                "properties", Document.fromMap(Map.of("days", days))));
    }
}
