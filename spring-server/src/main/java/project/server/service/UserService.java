package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.common.exception.DatabaseException;
import project.server.common.exception.UserException;
import project.server.dao.AlarmRepository;
import project.server.dao.UserRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.dao.entity.UserEntity;
import project.server.dto.user.*;

import java.util.Locale;
import project.server.util.jwt.JwtTokenProvider;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static project.server.common.response.status.BaseExceptionResponseStatus.*;

import java.time.LocalTime;
import java.util.List;

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

        validateEmail(postUserRequest.getEmail());
        String nickname = postUserRequest.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = nicknameFromEmail(postUserRequest.getEmail());
        }
        validateNickname(nickname);


        String encodedPassword = passwordEncoder.encode(postUserRequest.getPassword());
        postUserRequest.resetPassword(encodedPassword);

        UserEntity user = UserEntity.builder()
                .email(postUserRequest.getEmail())
                .password(postUserRequest.getPassword())
                .phoneNumber(postUserRequest.getPhoneNumber())
                .nickname(nickname)
                .profileImage(postUserRequest.getProfileImage())
                .status("active")
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

        String jwt = jwtTokenProvider.createToken(postUserRequest.getEmail(), saved.getUserId());

        return new PostUserResponse(saved.getUserId(), jwt);
    }

    public void modifyUserStatus_dormant(long userId) {
        log.info("[UserService.modifyUserStatus_dormant]");

        int affectedRows = userRepository.updateStatusByUserId(userId, "dormant");
        if (affectedRows != 1) {
            throw new DatabaseException(DATABASE_ERROR);
        }
    }

    public void modifyUserStatus_deleted(long userId) {
        log.info("[UserService.modifyUserStatus_deleted]");

        int affectedRows = userRepository.updateStatusByUserId(userId, "deleted");
        if (affectedRows != 1) {
            throw new DatabaseException(DATABASE_ERROR);
        }
    }

    public void modifyNickname(long userId, String nickname) {
        log.info("[UserService.modifyNickname]");

        validateNickname(nickname);
        int affectedRows = userRepository.updateNicknameByUserId(userId, nickname);
        if (affectedRows != 1) {
            throw new DatabaseException(DATABASE_ERROR);
        }
    }

    public List<GetUserResponse> getUsers(String nickname, String email, String status) {
        log.info("[UserService.getUsers]");
        return userRepository.searchUsers("%" + nickname + "%", "%" + email + "%", status);
    }

    public GetUserProfileResponse getProfile(long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
        return GetUserProfileResponse.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .profileImage(user.getProfileImage())
                .status(user.getStatus())
                .build();
    }

    public GetUserFitbitStatusResponse getFitbitStatus(long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
        boolean linked =
                (user.getFitbitAccessToken() != null && !user.getFitbitAccessToken().isBlank())
                        || (user.getFitbitRefreshToken() != null && !user.getFitbitRefreshToken().isBlank());
        return GetUserFitbitStatusResponse.builder()
                .linked(linked)
                .fitbitUserId(user.getFitbitUserId())
                .lastSyncedAt(user.getFitbitLastSyncedAt())
                .build();
    }

    private void validateEmail(String email) {
        if (userRepository.existsActiveOrDormantByEmail(email)) {
            throw new UserException(DUPLICATE_EMAIL);
        }
    }

    private void validateNickname(String nickname) {
        if (userRepository.existsActiveOrDormantByNickname(nickname)) {
            throw new UserException(DUPLICATE_NICKNAME);
        }
    }

    private static String nicknameFromEmail(String email) {
        int idx = email.indexOf('@');
        return (idx > 0 ? email.substring(0, idx) : email).trim().toLowerCase(Locale.ROOT);
    }
}
