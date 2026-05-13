package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.common.exception.UserException;
import project.server.dao.AlarmRepository;
import project.server.dao.FitbitRepository;
import project.server.dao.UserRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.dao.entity.FitbitEntity;
import project.server.dao.entity.UserEntity;
import project.server.dto.user.*;
import project.server.util.AlarmWakeAtHelper;

import project.server.util.jwt.JwtTokenProvider;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static project.server.common.response.status.BaseExceptionResponseStatus.*;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.stream.IntStream;

/**
 * 사용자 가입/프로필 서비스.
 *
 * <p>
 * 가입과 동시에 사용자별 알람 7건과 Fitbit 토큰 보관 행이 함께 생성된다.
 * 기본 알람은 <strong>한국(Asia/Seoul) 달력</strong> 기준 매주 해당 요일 <strong>07:30</strong>의
 * <em>아직 지나지 않은</em> 다음 순간으로 {@code dynamic_wake_at} 을 맞춘다.
 * Fitbit 토큰은 본래 가입자의 OAuth 동의를 거쳐 받아오지만,
 * 데모에서는 user_id=1 한정으로 미리 알고 있는 access/refresh token을 그대로 채워넣는다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** 데모용 하드코딩 토큰. user_id=1 가입 시점에만 fitbit 행에 함께 기록된다. */
    private static final long DEMO_USER_ID = 1L;
    private static final String DEMO_USER_FITBIT_ACCESS_TOKEN = "";
    private static final String DEMO_USER_FITBIT_REFRESH_TOKEN = "";

    private static final ZoneId ALARM_ZONE = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final AlarmRepository alarmRepository;
    private final FitbitRepository fitbitRepository;
    private final FitbitSyncService fitbitSyncService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public PostUserResponse signUp(PostUserRequest postUserRequest) {
        log.info("[UserService.signUp]");

        String nickname = postUserRequest.getNickname().trim();
        validateNickname(nickname);
        String encodedPassword = passwordEncoder.encode(postUserRequest.getPassword());
        postUserRequest.resetPassword(encodedPassword);

        UserEntity saved = userRepository.save(UserEntity.builder()
                .password(postUserRequest.getPassword())
                .nickname(nickname)
                .build());

        fitbitRepository.save(buildFitbitRow(saved.getUserId()));

        IntStream.rangeClosed(1, 7).forEach(day -> {
            LocalTime base = LocalTime.of(7, 30);
            alarmRepository.save(
                AlarmEntity.builder()
                        .userId(saved.getUserId())
                        .dayOfWeek(day)
                        .baseWakeTime(base)
                        .dynamicWakeAt(AlarmWakeAtHelper.nearestUpcomingWakeInstant(day, base, ALARM_ZONE))
                        .adaptiveEnabled(true)
                        .windowMinutesBefore(30)
                        .build());
        });

        // 가입 트랜잭션이 커밋된 직후, 비동기로 Fitbit 초기 적재를 트리거한다.
        // 별도 스레드에서 fitbit 행을 다시 읽어 refresh → 7일치 insert(존재 여부 무시) 를 수행한다.
        final Long newUserId = saved.getUserId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fitbitSyncService.initialSyncForUser(newUserId);
            }
        });

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

    /**
     * 가입한 사용자의 Fitbit 토큰 행을 만든다.
     * user_id=1 인 경우만 미리 알고 있는 토큰으로 채우고, 그 외에는 토큰이 비어 있는 상태로
     * 행만 생성한다. 토큰이 없는 사용자는 스케줄러에서 자동으로 스킵된다.
     */
    private FitbitEntity buildFitbitRow(Long userId) {
        FitbitEntity entity = FitbitEntity.builder().userId(userId).build();
        if (userId != null && userId == DEMO_USER_ID) {
            entity.setFitbitAccessToken(DEMO_USER_FITBIT_ACCESS_TOKEN);
            entity.setFitbitRefreshToken(DEMO_USER_FITBIT_REFRESH_TOKEN);
        }
        return entity;
    }
}
