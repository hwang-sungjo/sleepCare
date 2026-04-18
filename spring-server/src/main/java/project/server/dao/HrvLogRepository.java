package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.HrvLog;

import java.time.LocalDate;

public interface HrvLogRepository extends JpaRepository<HrvLog, Long> {
    void deleteByRecordDate(LocalDate recordDate);
    void deleteByRecordDateBefore(LocalDate cutoffDate);
}