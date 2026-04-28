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
@Tag(name = "Alarm", description = "기상 알람 조회/수정 API")
public class AlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "알람 조회", description = "현재 로그인 사용자의 기본 기상시간과 동적 알람 계산 결과를 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = GetAlarmResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT 검증 실패")
    })
    @GetMapping("")
    public BaseResponse<GetAlarmResponse> getAlarm(
            @Parameter(hidden = true) @PreAuthorize long userId) {
        log.info("[AlarmController.getAlarm] user={}", userId);
        return new BaseResponse<>(alarmService.getAlarm(userId));
    }

    @Operation(summary = "알람 설정 변경", description = "기본 기상시간, 적응형 모드, 탐색 윈도우를 변경하고 필요 시 동적 알람을 재계산합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = GetAlarmResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "JWT 검증 실패")
    })
    @PatchMapping("")
    public BaseResponse<GetAlarmResponse> patchAlarm(
            @Parameter(hidden = true) @PreAuthorize long userId,
            @Validated @RequestBody PatchAlarmRequest request, BindingResult bindingResult) {
        log.info("[AlarmController.patchAlarm] user={}", userId);
        if (bindingResult.hasErrors()) {
            throw new UserException(INVALID_USER_VALUE, getErrorMessages(bindingResult));
        }
        return new BaseResponse<>(alarmService.patchAlarm(userId, request));
    }
}
