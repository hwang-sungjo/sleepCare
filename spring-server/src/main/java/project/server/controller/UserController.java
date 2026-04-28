package project.server.controller;

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
public class UserController {

    private final UserService userService;

    /**
     * 내 프로필 조회
     */
    @GetMapping("/me")
    public BaseResponse<GetUserProfileResponse> me(@PreAuthorize long userId) {
        log.info("[UserController.me] user={}", userId);
        return new BaseResponse<>(userService.getProfile(userId));
    }

    /**
     * 회원 가입
     */
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