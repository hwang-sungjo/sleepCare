package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.dao.AlarmRepository;
import project.server.dao.FitbitDataRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.dao.entity.FitbitDataEntity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicAlarmService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");
    /** Fitbit-compatible stage identifiers considered amenable for a gentle wake */
    private static final Set<String> SHALLOW_STAGE_KEYWORDS = Set.of("light", "rem", "restless", "awake", "wake");

    private final AlarmRepository alarmRepository;
    private final FitbitDataRepository fitbitDataRepository;

    @Transactional
    public void recalculateForUser(Long userId) {
        int todayDay = LocalDate.now(DEFAULT_ZONE).getDayOfWeek().getValue();
        AlarmEntity alarm = alarmRepository.findByUserIdAndDayOfWeek(userId, todayDay).orElse(null);
        if (alarm == null || Boolean.FALSE.equals(alarm.getAdaptiveEnabled())) {
            return;
        }
        Instant now = Instant.now();
        Instant windowEnd = calculateWindowEndInstant(alarm, now);
        if (windowEnd == null) {
            alarm.setDynamicWakeAt(null);
            alarmRepository.save(alarm);
            return;
        }
        // 오늘 알람이 이미 울렸다면 다이나믹 값을 제거해 다음 주 동일 요일에서 다시 계산한다.
        if (hasRungAlready(alarm, now, windowEnd)) {
            alarm.setDynamicWakeAt(null);
            alarmRepository.save(alarm);
            return;
        }
        int windowMinutes = Objects.requireNonNullElse(alarm.getWindowMinutesBefore(), 30);
        Instant windowStart = windowEnd.minus(windowMinutes, ChronoUnit.MINUTES);

        List<FitbitDataEntity> slices = fitbitDataRepository.findByUserIdAndSegmentStartBetweenOrderBySegmentStartAsc(
                userId, windowStart, windowEnd.plus(1, ChronoUnit.MINUTES));

        Instant chosenInstant = slices.stream()
                .map(s -> shallowInstantInWindow(s, windowStart, windowEnd))
                .filter(java.util.Objects::nonNull)
                .filter(t -> !t.isBefore(now))
                .min(Comparator.naturalOrder())
                .orElse(windowEnd); // sensible default: ring at configured goal time when no granular data

        alarm.setDynamicWakeAt(chosenInstant);
        alarmRepository.save(alarm);
        log.debug("[DynamicAlarmService] user={} dynamicWakeAt={}", userId, chosenInstant);
    }

    /**
     * Returns the end instant of today's wake window anchored at
     * {@link AlarmEntity#getBaseWakeTime()} in Seoul,
     * only when "now" is still before that instant (alarm has not elapsed today).
     */
    private Instant calculateWindowEndInstant(AlarmEntity alarm, Instant reference) {
        LocalDate today = LocalDate.ofInstant(reference, DEFAULT_ZONE);
        LocalDateTime goal = LocalDateTime.of(today, alarm.getBaseWakeTime());
        Instant candidate = goal.atZone(DEFAULT_ZONE).toInstant();
        return candidate;
    }

    private static boolean hasRungAlready(AlarmEntity alarm, Instant now, Instant baseWakeInstant) {
        Instant todayDynamic = alarm.getDynamicWakeAt();
        if (todayDynamic != null) {
            return !now.isBefore(todayDynamic);
        }
        return !now.isBefore(baseWakeInstant);
    }

    private static Instant shallowInstantInWindow(FitbitDataEntity row, Instant windowStart, Instant windowEnd) {
        if (row.getSleepStage() == null) {
            return null;
        }
        String stage = row.getSleepStage().toLowerCase(Locale.ROOT);
        if (!isShallow(stage)) {
            return null;
        }
        Instant t = row.getSegmentStart();
        if (t == null) {
            return null;
        }
        if (t.isBefore(windowStart) || t.isAfter(windowEnd)) {
            return null;
        }
        return t;
    }

    /**
     * Identifies epochs that are comparatively light versus deep/restorative sleep
     * segments
     */
    private static boolean isShallow(String stageLower) {
        if (stageLower.contains("deep")) {
            return false;
        }
        return SHALLOW_STAGE_KEYWORDS.stream().anyMatch(stageLower::contains);
    }

}
