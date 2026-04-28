package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.common.exception.UserException;
import project.server.common.exception.jwt.unauthorized.JwtUnauthorizedTokenException;
import project.server.dao.UserRepository;
import project.server.dao.entity.UserEntity;
import project.server.dto.auth.LoginRequest;
import project.server.dto.auth.LoginResponse;
import project.server.util.jwt.JwtTokenProvider;

import static project.server.common.response.status.BaseExceptionResponseStatus.*;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest authRequest) {
        log.info("[AuthService.login]");

        String nickname = authRequest.getNickname();

        long userId = userRepository
                .findByNickname(nickname)
                .map(UserEntity::getUserId)
                .orElseThrow(() -> new UserException(EMAIL_NOT_FOUND));

        validatePassword(authRequest.getPassword(), userId);

        String updatedJwt = jwtTokenProvider.createToken(nickname, userId);

        return new LoginResponse(userId, updatedJwt);
    }

    private void validatePassword(String password, long userId) {
        String encodedPassword = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(EMAIL_NOT_FOUND))
                .getPassword();
        if (!passwordEncoder.matches(password, encodedPassword)) {
            throw new UserException(PASSWORD_NO_MATCH);
        }
    }

    public long getUserIdByEmail(String email) {
        return userRepository
                .findByNickname(email)
                .map(UserEntity::getUserId)
                .orElseThrow(() -> new JwtUnauthorizedTokenException(TOKEN_MISMATCH));
    }
}
