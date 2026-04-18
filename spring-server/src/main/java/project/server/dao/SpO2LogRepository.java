package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.SpO2Log;

import java.time.LocalDate;

public interface SpO2LogRepository extends JpaRepository<SpO2Log, Long> {
    void deleteByRecordDate(LocalDate recordDate);
    void deleteByRecordDateBefore(LocalDate cutoffDate);
}