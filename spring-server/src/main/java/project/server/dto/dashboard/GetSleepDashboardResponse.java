package project.server.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import project.server.dto.ai.CitationItem;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "홈 대시보드 수면 요약 응답 DTO")
public class GetSleepDashboardResponse {

    @Schema(
            description = "수면 효율(%) — 해당 summary 행 값. 존재하는 요약 행 하나를 사용하며 과거 평균 등은 아님.",
            example = "92")
    private Integer sleepEfficiencyPercent;

    @Schema(description = "수면 요약 행 하나의 수면 시간(분). 대시보드는 최근 존재하는 행 하나를 사용합니다.")
    private Integer averageSleepDurationMinutes;

    @Schema(
            description = "Bedrock 가 생성한 한국어 AI 분석·조언 텍스트. 일시적 실패 시 필드 자체가 생략됩니다.",
            nullable = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String aiAdvice;

    @Schema(description = "aiAdvice 생성 시 사용된 KB 인용(챗봇 citations 와 동일 구조). 없거나 실패 시 생략.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CitationItem> citations;
}
