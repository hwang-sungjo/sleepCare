package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.common.exception.UserException;
import project.server.dao.AlarmRepository;
import project.server.dao.UserRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.dao.entity.UserEntity;
import project.server.dto.user.*;

import project.server.util.jwt.JwtTokenProvider;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static project.server.common.response.status.BaseExceptionResponseStatus.*;

import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AlarmRepository alarmRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public PostUserResponse signUp(PostUserRequest postUserRequest) {
        log.info("[UserService.signUp]");

        String nickname = postUserRequest.getNickname().trim();
        validateNickname(nickname);
        String encodedPassword = passwordEncoder.encode(postUserRequest.getPassword());
        postUserRequest.resetPassword(encodedPassword);

        UserEntity user = UserEntity.builder()
                .password(postUserRequest.getPassword())
                .nickname(nickname)
                .build();

        UserEntity saved = userRepository.save(user);
        alarmRepository.save(
                AlarmEntity.builder()
                        .userId(saved.getUserId())
                        .baseWakeTime(LocalTime.of(7, 30))
                        .dynamicWakeAt(null)
                        .adaptiveEnabled(true)
                        .windowMinutesBefore(30)
                        .build());

        String jwt = jwtTokenProvider.createToken(nickname, saved.getUserId());

        return new PostUserResponse(saved.getUserId(), jwt);
    }

    public GetUserProfileResponse getProfile(long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
        return GetUserProfileResponse.builder()
                .nickname(user.getNickname())
                .build();
    }

    private void validateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new UserException(DUPLICATE_NICKNAME);
        }

    }
}
