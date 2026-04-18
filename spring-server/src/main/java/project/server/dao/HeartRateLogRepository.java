package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.HeartRateLog;

import java.time.LocalDate;

public interface HeartRateLogRepository extends JpaRepository<HeartRateLog, Long> {
    void deleteByRecordDate(LocalDate recordDate);
    void deleteByRecordDateBefore(LocalDate cutoffDate);
}