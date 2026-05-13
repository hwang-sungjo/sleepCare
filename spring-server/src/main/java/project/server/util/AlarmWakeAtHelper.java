package project.server.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 요일별 알람 행에서 {@code dynamic_wake_at} 채우기용 {@link LocalDateTime} 계산 도우미.
 * 모든 값은 {@code zone}(주로 Asia/Seoul) 기준 <strong>벽시계</strong>로 해석·반환한다.
 */
public final class AlarmWakeAtHelper {

    private AlarmWakeAtHelper() {
    }

    /**
     * ISO 요일 (1=월 … 7=일) + 기준 시각으로, 오늘부터 가장 빠르게 도래하는 그 요일·그 시각(KST 등).
     */
    public static LocalDateTime nearestOccurrenceWakeAt(int isoDayOfWeek1To7, LocalTime baseWakeTime, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        int curr = today.getDayOfWeek().getValue();
        int delta = Math.floorMod(isoDayOfWeek1To7 - curr, 7);
        LocalDate wakeDate = today.plusDays(delta);
        return LocalDateTime.of(wakeDate, baseWakeTime);
    }

    /**
     * {@link #nearestOccurrenceWakeAt} 과 같이 계산한 뒤, {@code zone} 기준 현재 시각을 지난 경우
     * 같은 요일·같은 벽시계 시각으로 주 단위로 밀어 아직 오지 않은 첫 순간을 반환한다.
     */
    public static LocalDateTime nearestUpcomingWakeAt(int isoDayOfWeek1To7, LocalTime baseWakeTime, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        int curr = today.getDayOfWeek().getValue();
        int delta = Math.floorMod(isoDayOfWeek1To7 - curr, 7);
        LocalDate wakeDate = today.plusDays(delta);
        ZonedDateTime zdt = ZonedDateTime.of(wakeDate, baseWakeTime, zone);
        LocalDateTime now = LocalDateTime.now(zone);
        while (!zdt.toLocalDateTime().isAfter(now)) {
            wakeDate = wakeDate.plusWeeks(1);
            zdt = ZonedDateTime.of(wakeDate, baseWakeTime, zone);
        }
        return zdt.toLocalDateTime();
    }

    /** 오늘(해당 zone)의 날짜 + base 시간. */
    public static LocalDateTime todayWakeAt(LocalTime baseWakeTime, ZoneId zone) {
        return LocalDateTime.of(LocalDate.now(zone), baseWakeTime);
    }

    /** 오늘 이후 해당 ISO 요일의 "다음" 발생(오늘이 그 요일이면 다음 주 동일 요일). */
    public static LocalDateTime nextWeeklyWakeAt(int isoDayOfWeek1To7, LocalTime baseWakeTime, ZoneId zone) {
        LocalDate start = LocalDate.now(zone);
        LocalDate wakeDate = start.with(TemporalAdjusters.next(DayOfWeek.of(isoDayOfWeek1To7)));
        return LocalDateTime.of(wakeDate, baseWakeTime);
    }
}
