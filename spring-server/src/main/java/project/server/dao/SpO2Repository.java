package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.SpO2;

import java.time.LocalDate;
import java.util.List;

public interface SpO2Repository extends JpaRepository<SpO2, Long> {

    /** 해당 일자의 분 단위 SpO2 행을 시간 오름차순으로 모두 가져온다 (AI 컨텍스트 빌드용). */
    List<SpO2> findByUserIdAndRecordDateOrderByRecordTimeAsc(Long userId, LocalDate recordDate);
}
