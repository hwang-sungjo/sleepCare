package project.server.dao;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.server.dao.entity.UserEntity;
import project.server.dto.user.GetUserResponse;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u WHERE "
            + "(u.fitbitRefreshToken IS NOT NULL AND LENGTH(TRIM(u.fitbitRefreshToken)) > 0) "
            + "OR (u.fitbitAccessToken IS NOT NULL AND LENGTH(TRIM(u.fitbitAccessToken)) > 0)")
    List<UserEntity> findUsersWithStoredFitbitCredential();

    Optional<UserEntity> findByEmailAndStatus(String email, String status);

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.email = :email AND u.status IN ('active','dormant')")
    boolean existsActiveOrDormantByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.nickname = :nickname AND u.status IN ('active','dormant')")
    boolean existsActiveOrDormantByNickname(@Param("nickname") String nickname);

    @Query("SELECT new project.server.dto.user.GetUserResponse(u.email, u.phoneNumber, u.nickname, u.profileImage, u.status) "
            + "FROM UserEntity u WHERE u.nickname LIKE :nickname AND u.email LIKE :email AND u.status = :status")
    List<GetUserResponse> searchUsers(
            @Param("nickname") String nickname,
            @Param("email") String email,
            @Param("status") String status);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.status = :status WHERE u.userId = :userId")
    int updateStatusByUserId(@Param("userId") long userId, @Param("status") String status);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.nickname = :nickname WHERE u.userId = :userId")
    int updateNicknameByUserId(@Param("userId") long userId, @Param("nickname") String nickname);
}
