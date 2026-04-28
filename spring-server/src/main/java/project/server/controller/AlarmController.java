package project.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import project.server.common.argument_resolver.PreAuthorize;
import project.server.common.exception.UserException;
import project.server.common.response.BaseResponse;
import project.server.dto.alarm.GetAlarmResponse;
import project.server.dto.alarm.PatchAlarmRequest;
import project.server.service.AlarmService;

import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static project.server.common.response.status.BaseExceptionResponseStatus.INVALID_USER_VALUE;
import static project.server.util.BindingResultUtils.getErrorMessages;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("")
    public BaseResponse<GetAlarmResponse> getAlarm(@PreAuthorize long userId) {
        log.info("[AlarmController.getAlarm] user={}", userId);
        return new BaseResponse<>(alarmService.getAlarm(userId));
    }

    @PatchMapping("")
    public BaseResponse<GetAlarmResponse> patchAlarm(@PreAuthorize long userId,
            @Validated @RequestBody PatchAlarmRequest request, BindingResult bindingResult) {
        log.info("[AlarmController.patchAlarm] user={}", userId);
        if (bindingResult.hasErrors()) {
            throw new UserException(INVALID_USER_VALUE, getErrorMessages(bindingResult));
        }
        return new BaseResponse<>(alarmService.patchAlarm(userId, request));
    }
}
