package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.HeartRate;

import java.time.LocalDate;
import java.util.Optional;

public interface HeartRateRepository extends JpaRepository<HeartRate, Long> {

    /**
     * (사용자, 날짜, 시각) 으로 정확히 한 행을 찾는다. realtime_metric backfill 시 분 단위로 매칭.
     * Fitbit intraday 응답의 record_time 은 항상 "HH:mm:ss" 포맷이라는 점을 가정한다.
     */
    Optional<HeartRate> findByUserIdAndRecordDateAndRecordTime(Long userId, LocalDate recordDate, String recordTime);

    boolean existsByUserIdAndRecordDateAndRecordTime(Long userId, LocalDate recordDate, String recordTime);
}
