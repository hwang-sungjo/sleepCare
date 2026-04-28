package project.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import project.server.common.argument_resolver.PreAuthorize;
import project.server.common.response.BaseResponse;
import project.server.dto.dashboard.GetSleepDashboardResponse;
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
            description = "수면 효율, 평균 수면 시간(분), 환경 힌트 텍스트를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetSleepDashboardResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT 검증 실패")
    })
    @GetMapping("/sleep-summary")
    public BaseResponse<GetSleepDashboardResponse> summary(
            @Parameter(hidden = true) @PreAuthorize long userId) {
        log.info("[DashboardController.summary] user={}", userId);
        return new BaseResponse<>(dashboardService.dashboard(userId));
    }
}
