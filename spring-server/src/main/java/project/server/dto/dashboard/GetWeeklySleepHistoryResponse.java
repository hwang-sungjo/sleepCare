package project.server.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "KST 기준 최근 7일(오늘 포함) 수면 상세 기록 목록")
public class GetWeeklySleepHistoryResponse {

    @Schema(description = "record_date 오름차순. DB에 존재하는 날만 포함됩니다.")
    private List<DailySleepRecordResponse> records;
}
