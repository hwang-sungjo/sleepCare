package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.HeartRate;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HeartRateRepository extends JpaRepository<HeartRate, Long> {

    /**
     * (사용자, 날짜, 해당 분의 절대 시각) 으로 정확히 한 행을 찾는다.
     */
    Optional<HeartRate> findByUserIdAndRecordDateAndRecordTime(Long userId, LocalDate recordDate, LocalDateTime recordTime);

    boolean existsByUserIdAndRecordDateAndRecordTime(Long userId, LocalDate recordDate, LocalDateTime recordTime);

    /** 해당 일자의 분 단위 심박 행을 시간 오름차순으로 모두 가져온다 (AI 컨텍스트 빌드용). */
    List<HeartRate> findByUserIdAndRecordDateOrderByRecordTimeAsc(Long userId, LocalDate recordDate);
}
