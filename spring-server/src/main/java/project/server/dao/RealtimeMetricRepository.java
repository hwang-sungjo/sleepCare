package project.server.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import project.server.dao.entity.RealtimeMetricEntity;

import java.time.Instant;

import java.util.List;

public interface RealtimeMetricRepository extends JpaRepository<RealtimeMetricEntity, Long> {

    List<RealtimeMetricEntity> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);

    /**
     * created_at 이 주어진 시각보다 이전인 모든 행을 한 번의 bulk DELETE 로 삭제한다.
     * 보존 기한 정리용. 반환값은 영향받은 행 수.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RealtimeMetricEntity r WHERE r.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
