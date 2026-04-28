package project.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import project.server.common.argument_resolver.PreAuthorize;
import project.server.common.exception.UserException;
import project.server.common.response.BaseResponse;
import project.server.dto.fitbit.FitbitOAuthExchangeRequest;
import project.server.dto.user.GetUserFitbitStatusResponse;
import project.server.service.FitbitIntegrationService;
import project.server.service.UserService;

import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static project.server.common.response.status.BaseExceptionResponseStatus.INVALID_USER_VALUE;
import static project.server.util.BindingResultUtils.getErrorMessages;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/fitbit")
public class FitbitController {

    private final FitbitIntegrationService fitbitIntegrationService;
    private final UserService userService;

    @PostMapping("/oauth/token")
    public BaseResponse<GetUserFitbitStatusResponse> exchange(@PreAuthorize long userId,
            @Validated @RequestBody FitbitOAuthExchangeRequest request, BindingResult bindingResult) {
        log.info("[FitbitController.exchange] user={}", userId);
        if (bindingResult.hasErrors()) {
            throw new UserException(INVALID_USER_VALUE, getErrorMessages(bindingResult));
        }
        fitbitIntegrationService.exchangeAuthorizationCode(userId, request);
        return new BaseResponse<>(userService.getFitbitStatus(userId));
    }

    @PostMapping("/sync/yesterday")
    public BaseResponse<String> syncYesterday(@PreAuthorize long userId) {
        log.info("[FitbitController.syncYesterday] user={}", userId);
        fitbitIntegrationService.syncYesterdayForUser(userId);
        return new BaseResponse<>("sync completed");
    }
}
