package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.SleepStageLog;

import java.time.LocalDate;

public interface SleepStageLogRepository extends JpaRepository<SleepStageLog, Long> {
    void deleteByRecordDate(LocalDate recordDate);
    void deleteByRecordDateBefore(LocalDate cutoffDate);
}