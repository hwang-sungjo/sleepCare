package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.DailyHealthSummary;
import java.time.LocalDate;

public interface DailyHealthSummaryRepository extends JpaRepository<DailyHealthSummary, LocalDate> {
    void deleteByRecordDateBefore(LocalDate cutoffDate);
}