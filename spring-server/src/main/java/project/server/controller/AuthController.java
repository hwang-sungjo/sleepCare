package project.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.common.exception.UserException;
import project.server.common.response.BaseErrorResponse;
import project.server.common.response.BaseResponse;
import project.server.dto.auth.LoginRequest;
import project.server.dto.auth.LoginResponse;
import project.server.service.AuthService;

import static project.server.common.response.status.BaseExceptionResponseStatus.INVALID_USER_VALUE;
import static project.server.util.BindingResultUtils.getErrorMessages;

import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth", description = "사용자 인증(로그인) API")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "로그인",
            description = "닉네임(userId)과 비밀번호를 검증하고 JWT를 발급합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패 또는 사용자/비밀번호 불일치",
                    content = @Content(
                            schema = @Schema(implementation = BaseErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ValidationFail",
                                            summary = "입력 누락",
                                            value = """
                                                    {
                                                      "code": 5000,
                                                      "status": 400,
                                                      "message": "userId: {NotBlank}",
                                                      "timestamp": "2026-04-28T19:20:15.123"
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "PasswordMismatch",
                                            summary = "비밀번호 불일치",
                                            value = """
                                                    {
                                                      "code": 4004,
                                                      "status": 400,
                                                      "message": "비밀번호가 일치하지 않습니다.",
                                                      "timestamp": "2026-04-28T19:20:15.123"
                                                    }
                                                    """)
                            }))
    })
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@Validated @RequestBody LoginRequest authRequest,
            BindingResult bindingResult) {
        log.info("[AuthController.login]");
        if (bindingResult.hasErrors()) {
            throw new UserException(INVALID_USER_VALUE, getErrorMessages(bindingResult));
        }
        return new BaseResponse<>(authService.login(authRequest));
    }

}