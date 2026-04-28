package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import project.server.dao.entity.AlarmEntity;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    Optional<AlarmEntity> findByUserIdAndDayOfWeek(long userId, int dayOfWeek);

    List<AlarmEntity> findAllByUserIdOrderByDayOfWeekAsc(long userId);
}
