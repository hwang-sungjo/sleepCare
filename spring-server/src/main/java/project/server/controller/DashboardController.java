package project.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import project.server.common.argument_resolver.PreAuthorize;
import project.server.common.response.BaseErrorResponse;
import project.server.common.response.BaseResponse;
import project.server.dto.dashboard.GetSleepDashboardResponse;
import project.server.dto.dashboard.GetWeeklySleepHistoryResponse;
import project.server.service.DashboardService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "홈 대시보드 표시용 요약 지표 API")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "수면 요약 조회",
            description =
                    "최근 7일(KST 기준 오늘부터 역방향) 중 daily_health_summary 가 존재하는 가장 새로운 날의 수면 지표와, "
                            + "최근 realtime_metric 에 기반한 환경 안내 문자열을 반환합니다. "
                            + "반드시 '어제 수면'을 의미하지는 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetSleepDashboardResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = BaseErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "TokenNotFound",
                                    value = """
                                            {
                                              "code": 4001,
                                              "status": 400,
                                              "message": "토큰이 HTTP Header에 없습니다.",
                                              "timestamp": "2026-04-28T19:20:15.123"
                                            }
                                            """)))
    })
    @GetMapping("/sleep-summary")
    public BaseResponse<GetSleepDashboardResponse> summary(
            @Parameter(hidden = true) @PreAuthorize long userId) {
        log.info("[DashboardController.summary] user={}", userId);
        return new BaseResponse<>(dashboardService.dashboard(userId));
    }

    @Operation(
            summary = "최근 7일 수면 상세 기록 조회",
            description =
                    "KST 기준 오늘을 포함한 최근 7일 구간에서 daily_health_summary 가 존재하는 날의 "
                            + "수면 효율·시간·수면 단계별 분을 record_date 오름차순으로 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetWeeklySleepHistoryResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = BaseErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "TokenNotFound",
                                    value = """
                                            {
                                              "code": 4001,
                                              "status": 400,
                                              "message": "토큰이 HTTP Header에 없습니다.",
                                              "timestamp": "2026-04-28T19:20:15.123"
                                            }
                                            """)))
    })
    @GetMapping("/sleep-history")
    public BaseResponse<GetWeeklySleepHistoryResponse> sleepHistory(
            @Parameter(hidden = true) @PreAuthorize long userId) {
        log.info("[DashboardController.sleepHistory] user={}", userId);
        return new BaseResponse<>(dashboardService.weeklySleepHistory(userId));
    }
}
