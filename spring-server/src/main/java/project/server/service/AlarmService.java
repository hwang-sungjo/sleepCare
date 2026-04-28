package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.dao.AlarmRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.dto.alarm.GetAlarmResponse;
import project.server.dto.alarm.PatchAlarmRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AlarmRepository alarmRepository;
    private final DynamicAlarmService dynamicAlarmService;

    public GetAlarmResponse getAlarm(long userId) {
        AlarmEntity alarm = alarmRepository
                .findByUserId(userId)
                .orElseGet(() -> alarmRepository.save(defaultAlarm(userId)));
        return toResponse(alarm);
    }

    @Transactional
    public GetAlarmResponse patchAlarm(long userId, PatchAlarmRequest request) {
        AlarmEntity alarm = alarmRepository
                .findByUserId(userId)
                .orElseGet(() -> alarmRepository.save(defaultAlarm(userId)));
        if (request.getAdaptiveEnabled() != null) {
            alarm.setAdaptiveEnabled(request.getAdaptiveEnabled());
        }
        if (request.getWindowMinutesBefore() != null) {
            alarm.setWindowMinutesBefore(request.getWindowMinutesBefore());
        }
        if (request.getBaseWakeTime() != null && !request.getBaseWakeTime().isBlank()) {
            alarm.setBaseWakeTime(LocalTime.parse(request.getBaseWakeTime(), TIME_FMT));
        }
        AlarmEntity saved = alarmRepository.save(alarm);
        if (Boolean.TRUE.equals(request.getRecomputeDynamicNow())) {
            dynamicAlarmService.recalculateForUser(userId);
            saved = alarmRepository.findByUserId(userId).orElse(saved);
        }
        return toResponse(saved);
    }

    private static AlarmEntity defaultAlarm(Long userId) {
        return AlarmEntity.builder()
                .userId(userId)
                .baseWakeTime(LocalTime.of(7, 30))
                .dynamicWakeAt(null)
                .adaptiveEnabled(true)
                .windowMinutesBefore(30)
                .build();
    }

    private static GetAlarmResponse toResponse(AlarmEntity alarm) {
        return GetAlarmResponse.builder()
                .baseWakeTime(alarm.getBaseWakeTime().format(TIME_FMT))
                .dynamicWakeAt(alarm.getDynamicWakeAt())
                .adaptiveEnabled(alarm.getAdaptiveEnabled())
                .windowMinutesBefore(alarm.getWindowMinutesBefore())
                .build();
    }
}
