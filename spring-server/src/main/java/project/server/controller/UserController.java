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
import project.server.dto.user.*;
import project.server.service.UserService;

import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static project.server.common.response.status.BaseExceptionResponseStatus.INVALID_USER_VALUE;
import static project.server.util.BindingResultUtils.getErrorMessages;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "User", description = "사용자 가입/프로필 API")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 프로필 조회",
            description = "JWT에서 해석한 사용자 식별자를 이용해 현재 로그인 사용자의 프로필을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetUserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT 없음/만료/형식 오류")
    })
    @GetMapping("/me")
    public BaseResponse<GetUserProfileResponse> me(
            @Parameter(hidden = true) @PreAuthorize long userId) {
        log.info("[UserController.me] user={}", userId);
        return new BaseResponse<>(userService.getProfile(userId));
    }

    @Operation(
            summary = "회원 가입",
            description = "닉네임/비밀번호로 사용자를 생성하고 즉시 사용 가능한 JWT를 반환합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "가입 성공",
                    content = @Content(schema = @Schema(implementation = PostUserResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류 또는 닉네임 중복")
    })
    @PostMapping("")
    public BaseResponse<PostUserResponse> signUp(@Validated @RequestBody PostUserRequest postUserRequest,
            BindingResult bindingResult) {
        log.info("[UserController.signUp]");
        if (bindingResult.hasErrors()) {
            throw new UserException(INVALID_USER_VALUE, getErrorMessages(bindingResult));
        }
        return new BaseResponse<>(userService.signUp(postUserRequest));
    }

}