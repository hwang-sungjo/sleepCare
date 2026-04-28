package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import project.server.dao.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByNickname(String nickname);

    boolean existsByNickname(String nickname);
}
