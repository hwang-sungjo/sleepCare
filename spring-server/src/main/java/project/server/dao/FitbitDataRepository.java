package project.server.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import project.server.dao.entity.FitbitDataEntity;

import java.time.Instant;
import java.util.List;

public interface FitbitDataRepository extends JpaRepository<FitbitDataEntity, Long> {

    long countByUserId(long userId);

    List<FitbitDataEntity> findByUserIdAndSegmentStartBetweenOrderBySegmentStartAsc(
            Long userId, Instant fromInclusive, Instant toInclusive);

    List<FitbitDataEntity> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);
}
