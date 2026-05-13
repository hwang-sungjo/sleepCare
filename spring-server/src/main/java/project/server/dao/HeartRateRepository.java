package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.HeartRate;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;

public interface HeartRateRepository extends JpaRepository<HeartRate, Long> {

    /**
     * (사용자, 날짜, 해당 분의 절대 시각) 으로 정확히 한 행을 찾는다.
     */
    Optional<HeartRate> findByUserIdAndRecordDateAndRecordTime(Long userId, LocalDate recordDate, LocalDateTime recordTime);

    boolean existsByUserIdAndRecordDateAndRecordTime(Long userId, LocalDate recordDate, LocalDateTime recordTime);
}
