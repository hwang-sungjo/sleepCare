package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.common.exception.UserException;
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

        UserEntity user = userRepository
                .findByNickname(nickname)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            throw new UserException(PASSWORD_NO_MATCH);
        }

        String updatedJwt = jwtTokenProvider.createToken(nickname, user.getUserId());

        return new LoginResponse(user.getUserId(), updatedJwt);
    }

}
