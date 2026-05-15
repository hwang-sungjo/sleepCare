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
import project.server.common.exception.UserException;
import project.server.common.response.BaseErrorResponse;
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

    @Operation(
            summary = "알람 조회",
            description =
                    "요일당 1행씩 저장된 설정을 불러오고 오늘 날짜(Asia/Seoul) 요일 행 기준 동적 알람을 재계산합니다. "
                            + "`todayEffectiveWakeAt`은 오늘 요일 행의 `dynamicWakeAt`(없거나 행이 없으면 null)입니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = GetAlarmResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = BaseErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "InvalidToken",
                                    value = """
                                            {
                                              "code": 4003,
                                              "status": 401,
                                              "message": "유효하지 않은 토큰입니다.",
                                              "timestamp": "2026-04-28T19:20:15.123"
                                            }
                                            """)))
    })
    @GetMapping("")
    public BaseResponse<GetAlarmResponse> getAlarm(
            @Parameter(hidden = true) @PreAuthorize long userId) {
        log.info("[AlarmController.getAlarm] user={}", userId);
        return new BaseResponse<>(alarmService.getAlarm(userId));
    }

    @Operation(
            summary = "알람 설정 변경",
            description =
                    "기본 벽시계 시각(LocalTime 문자열)·적응형 토글·탐색 윈도 분을 갱신하고, 오늘이 대상 요일이면 즉시 "
                            + "유효 dynamic_wake_at 을 채운 뒤 선택적으로 재계산 API를 타게 한다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = GetAlarmResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = BaseErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "InvalidDayOfWeek",
                                    value = """
                                            {
                                              "code": 5000,
                                              "status": 400,
                                              "message": "dayOfWeek: must be greater than or equal to 1",
                                              "timestamp": "2026-04-28T19:20:15.123"
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = BaseErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "ExpiredToken",
                                    value = """
                                            {
                                              "code": 4005,
                                              "status": 401,
                                              "message": "만료된 토큰입니다.",
                                              "timestamp": "2026-04-28T19:20:15.123"
                                            }
                                            """)))
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
