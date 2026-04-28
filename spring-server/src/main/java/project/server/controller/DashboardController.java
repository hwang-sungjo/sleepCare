package project.server.controller;

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
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/sleep-summary")
    public BaseResponse<GetSleepDashboardResponse> summary(@PreAuthorize long userId) {
        log.info("[DashboardController.summary] user={}", userId);
        return new BaseResponse<>(dashboardService.dashboard(userId));
    }
}
