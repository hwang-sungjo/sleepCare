package project.server.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import project.server.dao.entity.RealtimeMetricEntity;

import java.time.LocalDateTime;

import java.util.List;

public interface RealtimeMetricRepository extends JpaRepository<RealtimeMetricEntity, Long> {

    List<RealtimeMetricEntity> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);

    /** 지정 시각 범위의 환경 지표를 오름차순으로 가져온다 (4요인 알람 스코어링용). */
    List<RealtimeMetricEntity> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long userId, LocalDateTime start, LocalDateTime end);

    /**
     * created_at 이 주어진 시각보다 이전인 모든 행을 한 번의 bulk DELETE 로 삭제한다.
     * 보존 기한 정리용. 반환값은 영향받은 행 수.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RealtimeMetricEntity r WHERE r.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
