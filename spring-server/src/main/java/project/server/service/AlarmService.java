package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.dao.AlarmRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.dto.alarm.DailyAlarmItemResponse;
import project.server.dto.alarm.GetAlarmResponse;
import project.server.dto.alarm.PatchAlarmRequest;
import project.server.util.AlarmWakeAtHelper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * 요일별 기상 알람(사용자당 최대 7건) 조회 및 설정 변경.
 *
 * <p>
 * 날짜·요일 판별은 서울({@link ZoneId#of(String) Asia/Seoul}) 고정이다.
 * DB의 {@code base_wake_time} 은 같은 시각을 매 주 반복하는 패턴을 뜻하는 {@link LocalTime}이며,
 * {@code dynamic_wake_at} 은 그 패턴을 특정 일자와 합산해 계산된 실제 울림 후보 시각이다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    private final AlarmRepository alarmRepository;
    private final DynamicAlarmService dynamicAlarmService;
    /** lambda 프로파일에서는 MqttAlarmPublisher 빈이 없음 → Optional. */
    private final Optional<MqttAlarmPublisher> mqttAlarmPublisher;

    /**
     * 미보유 요일 알람 행을 채운 뒤 동적 알람을 재계산하고, 전체 목록과 오늘의 유효 기상 시각을 반환한다.
     */
    public GetAlarmResponse getAlarm(long userId) {
        ensureWeeklyAlarms(userId);
        int todayDay = LocalDate.now(DEFAULT_ZONE).getDayOfWeek().getValue();
        dynamicAlarmService.recalculateForUserAndDay(userId, todayDay);
        List<AlarmEntity> alarms = alarmRepository.findAllByUserIdOrderByDayOfWeekAsc(userId);
        LocalDateTime effective = resolveTodayEffectiveWakeAt(alarms, todayDay);
        return toResponse(alarms, todayDay, effective);
    }

    /**
     * 지정 요일 행을 갱신한다. 적응형 옵션·기준 시각·윈도우 분 변경 시 동일 트랜잭션에서 저장하고,
     * 오늘 요일만 {@code recomputeDynamicNow}=true 로 적응형 재계산을 요청할 수 있다.
     */
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
                alarm.setDynamicWakeAt(AlarmWakeAtHelper.nearestUpcomingWakeAt(
                        alarm.getDayOfWeek(), alarm.getBaseWakeTime(), DEFAULT_ZONE));
            }
        } else {
            alarm.setDynamicWakeAt(AlarmWakeAtHelper.nearestUpcomingWakeAt(
                    alarm.getDayOfWeek(), alarm.getBaseWakeTime(), DEFAULT_ZONE));
        }
        alarmRepository.save(alarm);

        boolean willRecalculateToday =
                Boolean.TRUE.equals(request.getRecomputeDynamicNow()) && alarm.getDayOfWeek() == todayDow;
        if (!willRecalculateToday && alarm.getDayOfWeek() == todayDow) {
            mqttAlarmPublisher.ifPresent(pub -> pub.publishWakeSchedule(userId, alarm.getDynamicWakeAt()));
        }

        if (Boolean.TRUE.equals(request.getRecomputeDynamicNow()) && alarm.getDayOfWeek() == todayDow) {
            dynamicAlarmService.recalculateForUserAndDay(userId, todayDow);
        }
        List<AlarmEntity> alarms = alarmRepository.findAllByUserIdOrderByDayOfWeekAsc(userId);
        int todayDay = LocalDate.now(DEFAULT_ZONE).getDayOfWeek().getValue();
        LocalDateTime effective = resolveTodayEffectiveWakeAt(alarms, todayDay);
        return toResponse(alarms, todayDay, effective);
    }

    /**
     * ISO 요일 1~7 각각 한 행씩 존재하도록 없는 슬롯만 기본값으로 생성한다.
     */
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

    /** 새 요일 슬롯의 기준 시각을 07:30으로 두고, 해당 요일에 가까운 발생 시각을 동적값으로 채운다. */
    private static AlarmEntity defaultAlarm(Long userId, int dayOfWeek) {
        LocalTime base = LocalTime.of(7, 30);
        return AlarmEntity.builder()
                .userId(userId)
                .dayOfWeek(dayOfWeek)
                .baseWakeTime(base)
                .dynamicWakeAt(AlarmWakeAtHelper.nearestUpcomingWakeAt(dayOfWeek, base, DEFAULT_ZONE))
                .adaptiveEnabled(true)
                .windowMinutesBefore(30)
                .build();
    }

    /**
     * 오늘 요일에 해당하는 알람 행의 {@link AlarmEntity#getDynamicWakeAt()} 을 반환한다.
     * 행이 없으면 null.
     */
    private static LocalDateTime resolveTodayEffectiveWakeAt(List<AlarmEntity> alarms, int todayDay) {
        return alarms.stream()
                .filter(a -> a.getDayOfWeek() == todayDay)
                .findFirst()
                .map(AlarmEntity::getDynamicWakeAt)
                .orElse(null);
    }

    private static GetAlarmResponse toResponse(List<AlarmEntity> alarms, int todayDay, LocalDateTime todayEffectiveWakeAt) {
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
