package project.server.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 요일별 알람 행에서 {@code dynamic_wake_at} 채우기용 {@link Instant} 계산 도우미.
 * 모든 오버로드는 호출부가 명시적으로 넘긴 {@link ZoneId}(주로 Seoul) 과 {@link LocalTime} 을 묶어 Zoned 순간으로 만든 뒤
 * 절대 시각({@link Instant})으로 축약한다.
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
        return ZonedDateTime.of(wakeDate, baseWakeTime, zone).toInstant();
    }

    /**
     * {@link #nearestOccurrenceWakeInstant} 와 같이 계산한 뒤, {@code zone} 기준 현재 시각을 지난 경우
     * 같은 요일·같은 벽시계 시각으로 주 단위로 밀어 <strong>아직 도래하지 않은</strong> 첫 순간을 반환한다.
     * (가입 시 기본 알람 등 “다음 울림 후보”에 사용.)
     */
    public static Instant nearestUpcomingWakeInstant(int isoDayOfWeek1To7, LocalTime baseWakeTime, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        int curr = today.getDayOfWeek().getValue();
        int delta = Math.floorMod(isoDayOfWeek1To7 - curr, 7);
        LocalDate wakeDate = today.plusDays(delta);
        ZonedDateTime zdt = ZonedDateTime.of(wakeDate, baseWakeTime, zone);
        Instant now = Instant.now();
        while (!zdt.toInstant().isAfter(now)) {
            wakeDate = wakeDate.plusWeeks(1);
            zdt = ZonedDateTime.of(wakeDate, baseWakeTime, zone);
        }
        return zdt.toInstant();
    }

    /** 오늘(KST 등) 의 날짜 + base 시간. */
    public static Instant todayWakeInstant(LocalTime baseWakeTime, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        return ZonedDateTime.of(today, baseWakeTime, zone).toInstant();
    }

    /** 오늘 이후 해당 ISO 요일의 "다음" 발생(오늘이 그 요일이면 다음 주 동일 요일). */
    public static Instant nextWeeklyWakeInstant(int isoDayOfWeek1To7, LocalTime baseWakeTime, ZoneId zone) {
        LocalDate start = LocalDate.now(zone);
        LocalDate wakeDate =
                start.with(TemporalAdjusters.next(DayOfWeek.of(isoDayOfWeek1To7)));
        return ZonedDateTime.of(wakeDate, baseWakeTime, zone).toInstant();
    }
}
