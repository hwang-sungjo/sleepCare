package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.Hrv;

import java.time.LocalDate;
import java.util.List;

public interface HrvRepository extends JpaRepository<Hrv, Long> {

    /** 해당 일자의 분 단위 HRV(rmssd) 행을 시간 오름차순으로 모두 가져온다 (AI 컨텍스트 빌드용). */
    List<Hrv> findByUserIdAndRecordDateOrderByRecordTimeAsc(Long userId, LocalDate recordDate);
}
