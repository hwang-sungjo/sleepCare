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
        AlarmEntity alarm = alarmRepository.findByUserId(userId).orElse(null);
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
        int windowMinutes = Objects.requireNonNullElse(alarm.getWindowMinutesBefore(), 30);
        Instant windowStart = windowEnd.minus(windowMinutes, ChronoUnit.MINUTES);

        List<FitbitDataEntity> slices = fitbitDataRepository.findByUserIdAndLoggedAtBetweenOrderByLoggedAtAsc(
                userId, windowStart, windowEnd.plus(1, ChronoUnit.MINUTES));

        Instant chosenInstant = slices.stream()
                .map(s -> shallowInstantInWindow(s, windowStart, windowEnd))
                .filter(java.util.Objects::nonNull)
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
        return reference.isBefore(candidate) ? candidate : null;
    }

    private static Instant shallowInstantInWindow(FitbitDataEntity row, Instant windowStart, Instant windowEnd) {
        if (row.getSleepStage() == null) {
            return null;
        }
        String stage = row.getSleepStage().toLowerCase(Locale.ROOT);
        if (!isShallow(stage)) {
            return null;
        }
        Instant t = Objects.requireNonNullElse(row.getSegmentStart(), row.getLoggedAt());
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
