package project.server.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import project.server.dao.entity.SensorDataEntity;

import java.util.List;

public interface SensorDataRepository extends JpaRepository<SensorDataEntity, Long> {

    List<SensorDataEntity> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);
}
