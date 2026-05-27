package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateResponse;

import java.util.Optional;

/**
 * 홈 대시보드용 AI 조언 텍스트 생성.
 *
 * <p>
 * S3 의 {@code skill_dashboard.md} 페르소나 + 서버가 구성한 사용자 메트릭 컨텍스트를
 * Bedrock RAG 로 보내 한국어 조언을 받아온다. 호출 실패 시 예외를 swallow 하고
 * {@link Optional#empty()} 를 돌려 대시보드 응답 자체의 가용성을 유지한다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAiAdviceService {

    private final S3PromptService s3PromptService;
    private final UserMetricsSummaryService userMetricsSummaryService;
    private final BedrockKnowledgeBaseService bedrockKnowledgeBaseService;

    @Value("${app.ai.prompt-key-dashboard}")
    private String promptKey;

    /**
     * 사용자 메트릭에 근거한 AI 조언 텍스트.
     * Bedrock/S3 호출 실패는 모두 흡수하고 {@link Optional#empty()} 를 반환.
     */
    public Optional<String> advise(long userId) {
        try {
            String systemPrompt = s3PromptService.getPrompt(promptKey);
            String userContext = userMetricsSummaryService.buildContext(userId);
            String userText = userContext
                    + "\n위 데이터를 근거로 한국어로 분석과 조언을 자연스러운 문단으로 답해 주세요.";

            RetrieveAndGenerateResponse response =
                    bedrockKnowledgeBaseService.retrieveAndGenerate(userText, systemPrompt, null);
            String text = response.output() == null ? null : response.output().text();
            if (text == null || text.isBlank()) {
                log.warn("[DashboardAiAdviceService] empty bedrock response userId={}", userId);
                return Optional.empty();
            }
            return Optional.of(text.trim());
        } catch (RuntimeException e) {
            log.warn("[DashboardAiAdviceService] advice generation failed userId={}: {}",
                    userId, e.getMessage());
            return Optional.empty();
        }
    }
}
