package project.server.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import project.server.dao.entity.RealtimeMetricEntity;

import java.util.List;

public interface RealtimeMetricRepository extends JpaRepository<RealtimeMetricEntity, Long> {

    List<RealtimeMetricEntity> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);
}
