package project.server.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * 요일별 알람의 {@code dynamic_wake_at} 에 넣을 기준 {@link Instant} 계산 (KST 등 zone 기준).
 */
public final class AlarmWakeAtHelper {

    private AlarmWakeAtHelper() {
    }

    /**
     * ISO 요일 (1=월 … 7=일) + 기준 시각 으로, 오늘부터 가장 빠르게 도래하는 그 요일 저녁(또는 아침) 시각 Instant.
     * 오늘이 해당 요일이면 오늘 날짜를 사용한다.
     */
    public static Instant nearestOccurrenceWakeInstant(int isoDayOfWeek1To7, LocalTime baseWakeTime, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        int curr = today.getDayOfWeek().getValue();
        int delta = Math.floorMod(isoDayOfWeek1To7 - curr, 7);
        LocalDate wakeDate = today.plusDays(delta);
        return LocalDateTime.of(wakeDate, baseWakeTime).atZone(zone).toInstant();
    }

    /** 오늘(KST 등) 의 날짜 + base 시간. */
    public static Instant todayWakeInstant(LocalTime baseWakeTime, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        return LocalDateTime.of(today, baseWakeTime).atZone(zone).toInstant();
    }

    /** 오늘 이후 해당 ISO 요일의 "다음" 발생(오늘이 그 요일이면 다음 주 동일 요일). */
    public static Instant nextWeeklyWakeInstant(int isoDayOfWeek1To7, LocalTime baseWakeTime, ZoneId zone) {
        LocalDate start = LocalDate.now(zone);
        LocalDate wakeDate =
                start.with(TemporalAdjusters.next(DayOfWeek.of(isoDayOfWeek1To7)));
        return LocalDateTime.of(wakeDate, baseWakeTime).atZone(zone).toInstant();
    }
}
