package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.DailyHealthSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyHealthSummaryRepository
        extends JpaRepository<DailyHealthSummary, DailyHealthSummary.PK> {

    Optional<DailyHealthSummary> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    boolean existsByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    List<DailyHealthSummary> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate);
}
