package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.common.exception.DatabaseException;
import project.server.common.exception.UserException;
import project.server.dao.UserDao;
import project.server.dto.user.*;
import project.server.util.jwt.JwtTokenProvider;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static project.server.common.response.status.BaseExceptionResponseStatus.*;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public PostUserResponse signUp(PostUserRequest postUserRequest) {
        log.info("[UserService.createUser]");

        validateEmail(postUserRequest.getEmail());
        String nickname = postUserRequest.getNickname();
        if (nickname != null) {
            validateNickname(postUserRequest.getNickname());
        }

        String encodedPassword = passwordEncoder.encode(postUserRequest.getPassword());
        postUserRequest.resetPassword(encodedPassword);

        long userId = userDao.createUser(postUserRequest);

        String jwt = jwtTokenProvider.createToken(postUserRequest.getEmail(), userId);

        return new PostUserResponse(userId, jwt);
    }

    public void modifyUserStatus_dormant(long userId) {
        log.info("[UserService.modifyUserStatus_dormant]");

        int affectedRows = userDao.modifyUserStatus_dormant(userId);
        if (affectedRows != 1) {
            throw new DatabaseException(DATABASE_ERROR);
        }
    }

    public void modifyUserStatus_deleted(long userId) {
        log.info("[UserService.modifyUserStatus_deleted]");

        int affectedRows = userDao.modifyUserStatus_deleted(userId);
        if (affectedRows != 1) {
            throw new DatabaseException(DATABASE_ERROR);
        }
    }

    public void modifyNickname(long userId, String nickname) {
        log.info("[UserService.modifyNickname]");

        validateNickname(nickname);
        int affectedRows = userDao.modifyNickname(userId, nickname);
        if (affectedRows != 1) {
            throw new DatabaseException(DATABASE_ERROR);
        }
    }

    public List<GetUserResponse> getUsers(String nickname, String email, String status) {
        log.info("[UserService.getUsers]");
        return userDao.getUsers(nickname, email, status);
    }

    private void validateEmail(String email) {
        if (userDao.hasDuplicateEmail(email)) {
            throw new UserException(DUPLICATE_EMAIL);
        }
    }

    private void validateNickname(String nickname) {
        if (userDao.hasDuplicateNickName(nickname)) {
            throw new UserException(DUPLICATE_NICKNAME);
        }
    }

}