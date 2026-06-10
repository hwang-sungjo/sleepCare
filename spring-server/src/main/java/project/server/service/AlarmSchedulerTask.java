package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.server.dao.AlarmRepository;
import project.server.dao.entity.AlarmEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmSchedulerTask {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");
    
    private final AlarmRepository alarmRepository;
    private final DynamicAlarmService dynamicAlarmService;

    /**
     * 1분마다 실행되며, 현재 시간이 설정된 기상 시간의 정확히 1시간 전인 알람들을 찾아
     * 동적 알람 시간을 계산하고 라즈베리파이로 전송합니다.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scheduleDynamicAlarms() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        LocalDateTime targetTime = now.plusHours(1);
        int targetDow = targetTime.getDayOfWeek().getValue();
        LocalTime targetLocalTime = targetTime.toLocalTime().withSecond(0).withNano(0);

        List<AlarmEntity> targetAlarms = alarmRepository.findAllByDayOfWeek(targetDow);
        if (targetAlarms == null || targetAlarms.isEmpty()) {
            return;
        }

        for (AlarmEntity alarm : targetAlarms) {
            LocalTime baseWakeTime = alarm.getBaseWakeTime();
            if (baseWakeTime == null) {
                continue;
            }

            LocalTime baseTimeTruncated = baseWakeTime.withSecond(0).withNano(0);
            
            if (targetLocalTime.equals(baseTimeTruncated)) {
                log.info("[AlarmSchedulerTask] Triggering dynamic alarm calculation for user {} 1 hour before baseWakeTime {}", alarm.getUserId(), baseWakeTime);
                try {
                    dynamicAlarmService.recalculateForUserAndDay(alarm.getUserId(), targetDow);
                } catch (Exception e) {
                    log.error("[AlarmSchedulerTask] Error calculating dynamic alarm for user {}", alarm.getUserId(), e);
                }
            }
        }
    }
}
