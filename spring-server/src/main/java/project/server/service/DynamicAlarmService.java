package project.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.server.dao.AlarmRepository;
import project.server.dao.SleepStageRepository;
import project.server.dao.entity.AlarmEntity;
import project.server.entity.SleepStage;
import project.server.util.AlarmWakeAtHelper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 적응형 알람의 동적 시각을 계산한다.
 *
 * <p>오늘의 기준 기상 시각을 기준으로 [- windowMinutes, 0] 구간 안에서
 * Fitbit 수면 단계가 "얕은(light/rem/awake)" 분기로 잡혀 있는 가장 빠른 시각을 선택한다.
 * 데이터가 충분하지 않으면 기본 기상 시각을 그대로 사용한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicAlarmService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");
    /** Fitbit-compatible stage identifiers considered amenable for a gentle wake */
    private static final Set<String> SHALLOW_STAGE_KEYWORDS = Set.of("light", "rem", "restless", "awake", "wake");

    private final AlarmRepository alarmRepository;
    private final SleepStageRepository sleepStageRepository;

    @Transactional
    public void recalculateForUser(Long userId) {
        int todayDay = LocalDate.now(DEFAULT_ZONE).getDayOfWeek().getValue();
        AlarmEntity alarm = alarmRepository.findByUserIdAndDayOfWeek(userId, todayDay).orElse(null);
        if (alarm == null) {
            return;
        }
        if (Boolean.FALSE.equals(alarm.getAdaptiveEnabled())) {
            alarm.setDynamicWakeAt(
                    AlarmWakeAtHelper.todayWakeInstant(alarm.getBaseWakeTime(), DEFAULT_ZONE));
            alarmRepository.save(alarm);
            return;
        }
        Instant now = Instant.now();
        Instant windowEnd = calculateWindowEndInstant(alarm, now);
        // 오늘 알람이 이미 울렸다면 다음 주 동일 요일 기준 시각으로 옮긴다.
        if (hasRungAlready(alarm, now, windowEnd)) {
            alarm.setDynamicWakeAt(AlarmWakeAtHelper.nextWeeklyWakeInstant(
                    alarm.getDayOfWeek(), alarm.getBaseWakeTime(), DEFAULT_ZONE));
            alarmRepository.save(alarm);
            return;
        }
        int windowMinutes = Objects.requireNonNullElse(alarm.getWindowMinutesBefore(), 30);
        Instant windowStart = windowEnd.minus(windowMinutes, ChronoUnit.MINUTES);

        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        List<SleepStage> stages = sleepStageRepository
                .findByUserIdAndRecordDateOrderByStartTimeAsc(userId, today);

        Instant chosenInstant = stages.stream()
                .map(s -> shallowInstantInWindow(s, windowStart, windowEnd))
                .filter(Objects::nonNull)
                .filter(t -> !t.isBefore(now))
                .min(Comparator.naturalOrder())
                .orElse(windowEnd);

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
        return ZonedDateTime.of(today, alarm.getBaseWakeTime(), DEFAULT_ZONE).toInstant();
    }

    private static boolean hasRungAlready(AlarmEntity alarm, Instant now, Instant baseWakeInstant) {
        Instant todayDynamic = alarm.getDynamicWakeAt();
        if (todayDynamic != null) {
            return !now.isBefore(todayDynamic);
        }
        return !now.isBefore(baseWakeInstant);
    }

    private static Instant shallowInstantInWindow(SleepStage row, Instant windowStart, Instant windowEnd) {
        if (row.getStageLevel() == null) {
            return null;
        }
        String stage = row.getStageLevel().toLowerCase(Locale.ROOT);
        if (!isShallow(stage)) {
            return null;
        }
        Instant t = row.getStartTime();
        if (t == null) {
            return null;
        }
        if (t.isBefore(windowStart) || t.isAfter(windowEnd)) {
            return null;
        }
        // Fitbit 의 stage 가 windowEnd 직후에 끝나는 경우에도 의미가 있도록 시작점만으로 가벼운 판정
        long durationSec = row.getDurationSeconds() == null ? 0 : row.getDurationSeconds();
        Instant segmentEnd = t.plus(Duration.ofSeconds(durationSec));
        if (segmentEnd.isBefore(windowStart)) {
            return null;
        }
        return t.isBefore(windowStart) ? windowStart : t;
    }

    private static boolean isShallow(String stageLower) {
        if (stageLower.contains("deep")) {
            return false;
        }
        return SHALLOW_STAGE_KEYWORDS.stream().anyMatch(stageLower::contains);
    }
}
