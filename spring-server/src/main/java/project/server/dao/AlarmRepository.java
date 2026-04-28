package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import project.server.dao.entity.AlarmEntity;

import java.util.Optional;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    Optional<AlarmEntity> findByUserId(long userId);
}
