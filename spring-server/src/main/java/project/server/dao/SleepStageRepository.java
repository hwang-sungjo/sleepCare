package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.SleepStage;

import java.time.LocalDate;
import java.util.List;

public interface SleepStageRepository extends JpaRepository<SleepStage, Long> {

    List<SleepStage> findByUserIdAndRecordDateOrderByStartTimeAsc(Long userId, LocalDate recordDate);
    SleepStage findFirstByUserIdOrderByRecordDateDesc(Long userId);
}
