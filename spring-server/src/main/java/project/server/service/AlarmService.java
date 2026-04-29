package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.dao.AlarmRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.dto.alarm.DailyAlarmItemResponse;
import project.server.util.AlarmWakeAtHelper;
import project.server.dto.alarm.GetAlarmResponse;
import project.server.dto.alarm.PatchAlarmRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    private final AlarmRepository alarmRepository;
    private final DynamicAlarmService dynamicAlarmService;

    public GetAlarmResponse getAlarm(long userId) {
        ensureWeeklyAlarms(userId);
        dynamicAlarmService.recalculateForUser(userId);
        List<AlarmEntity> alarms = alarmRepository.findAllByUserIdOrderByDayOfWeekAsc(userId);
        int todayDay = LocalDate.now(DEFAULT_ZONE).getDayOfWeek().getValue();
        Instant effective = resolveTodayEffectiveWakeAt(alarms, todayDay);
        return toResponse(alarms, todayDay, effective);
    }

    @Transactional
    public GetAlarmResponse patchAlarm(long userId, PatchAlarmRequest request) {
        ensureWeeklyAlarms(userId);
        int day = request.getDayOfWeek();
        AlarmEntity alarm = alarmRepository
                .findByUserIdAndDayOfWeek(userId, day)
                .orElseGet(() -> alarmRepository.save(defaultAlarm(userId, day)));
        if (request.getAdaptiveEnabled() != null) {
            alarm.setAdaptiveEnabled(request.getAdaptiveEnabled());
        }
        if (request.getWindowMinutesBefore() != null) {
            alarm.setWindowMinutesBefore(request.getWindowMinutesBefore());
        }
        if (request.getBaseWakeTime() != null && !request.getBaseWakeTime().isBlank()) {
            alarm.setBaseWakeTime(LocalTime.parse(request.getBaseWakeTime(), TIME_FMT));
        }
        int todayDow = LocalDate.now(DEFAULT_ZONE).getDayOfWeek().getValue();
        if (alarm.getDayOfWeek() == todayDow) {
            if (!Boolean.TRUE.equals(request.getRecomputeDynamicNow())) {
                alarm.setDynamicWakeAt(AlarmWakeAtHelper.todayWakeInstant(alarm.getBaseWakeTime(), DEFAULT_ZONE));
            }
        } else {
            alarm.setDynamicWakeAt(AlarmWakeAtHelper.nearestOccurrenceWakeInstant(
                    alarm.getDayOfWeek(), alarm.getBaseWakeTime(), DEFAULT_ZONE));
        }
        alarmRepository.save(alarm);

        if (Boolean.TRUE.equals(request.getRecomputeDynamicNow()) && alarm.getDayOfWeek() == todayDow) {
            dynamicAlarmService.recalculateForUser(userId);
        }
        List<AlarmEntity> alarms = alarmRepository.findAllByUserIdOrderByDayOfWeekAsc(userId);
        int todayDay = LocalDate.now(DEFAULT_ZONE).getDayOfWeek().getValue();
        Instant effective = resolveTodayEffectiveWakeAt(alarms, todayDay);
        return toResponse(alarms, todayDay, effective);
    }

    @Transactional
    protected void ensureWeeklyAlarms(long userId) {
        List<AlarmEntity> alarms = alarmRepository.findAllByUserIdOrderByDayOfWeekAsc(userId);
        if (alarms.size() >= 7) {
            return;
        }
        IntStream.rangeClosed(1, 7).forEach(day -> {
            if (alarmRepository.findByUserIdAndDayOfWeek(userId, day).isEmpty()) {
                alarmRepository.save(defaultAlarm(userId, day));
            }
        });
    }

    private static AlarmEntity defaultAlarm(Long userId, int dayOfWeek) {
        LocalTime base = LocalTime.of(7, 30);
        return AlarmEntity.builder()
                .userId(userId)
                .dayOfWeek(dayOfWeek)
                .baseWakeTime(base)
                .dynamicWakeAt(AlarmWakeAtHelper.nearestOccurrenceWakeInstant(dayOfWeek, base, DEFAULT_ZONE))
                .adaptiveEnabled(true)
                .windowMinutesBefore(30)
                .build();
    }

    private static Instant resolveTodayEffectiveWakeAt(List<AlarmEntity> alarms, int todayDay) {
        return alarms.stream()
                .filter(a -> a.getDayOfWeek() == todayDay)
                .findFirst()
                .map(AlarmEntity::getDynamicWakeAt)
                .orElse(null);
    }

    private static GetAlarmResponse toResponse(List<AlarmEntity> alarms, int todayDay, Instant todayEffectiveWakeAt) {
        List<DailyAlarmItemResponse> items = alarms.stream()
                .map(alarm -> DailyAlarmItemResponse.builder()
                        .dayOfWeek(alarm.getDayOfWeek())
                        .baseWakeTime(alarm.getBaseWakeTime().format(TIME_FMT))
                        .dynamicWakeAt(alarm.getDynamicWakeAt())
                        .adaptiveEnabled(alarm.getAdaptiveEnabled())
                        .windowMinutesBefore(alarm.getWindowMinutesBefore())
                        .build())
                .toList();
        return GetAlarmResponse.builder()
                .todayDayOfWeek(todayDay)
                .todayEffectiveWakeAt(todayEffectiveWakeAt)
                .alarms(items)
                .build();
    }
}
