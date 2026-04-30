package project.server.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Fitbit REST JSON 에서 문자열 형태로 내려오는 시각을 {@link Instant} 하나로 고정하기 위한 파서 묶음.
 *
 * <p>
 * 타임존이 없는 문자열은 서울({@link #DEFAULT_PARSE_ZONE}) 달력 벽 위에 놓였다고 가정하여 변환한다.
 * 심박 intraday 는 "해당 로컬일 + 시:분(:초)" 조합이라 별도 메서드를 둔다.
 * </p>
 */
public final class FitbitInstantParser {

    /** 오프셋 없는 문자열 로컬 날시 해석 시 사용한다. */
    private static final ZoneId DEFAULT_PARSE_ZONE = ZoneId.of("Asia/Seoul");

    private FitbitInstantParser() {
    }

    /**
     * 수면 시작/종료, SpO2·HRV 분 타임스탬프 등 Fitbit 형식 문자열을 {@link Instant} 로 변환한다.
     *
     * <p>
     * 시도 순서: RFC/ISO 형태 {@link OffsetDateTime}, 풀 Zoned 문자열 {@link ZonedDateTime},
     * 마지막으로 오프셋 없는 로컬 {@link LocalDateTime} 을 {@link #DEFAULT_PARSE_ZONE} 에 두고 변환.
     * </p>
     */
    public static Optional<Instant> parseFlexibleInstant(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(text).toInstant());
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Optional.of(ZonedDateTime.parse(text).toInstant());
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Optional.of(LocalDateTime.parse(text).atZone(DEFAULT_PARSE_ZONE).toInstant());
        } catch (DateTimeParseException ignored) {
        }
        return Optional.empty();
    }

    /**
     * 심박 1분 intraday 의 {@code time} 필드를 지정 로컬일과 묶어 {@link Instant} 로 만든다.
     *
     * <p>
     * {@code HH:mm} 형만 오는 경우는 초 단위까지 맞춰 파싱을 재시도한다.
     * </p>
     */
    public static Instant parseHeartRateMinuteToInstant(LocalDate recordDate, String timeField, ZoneId zone) {
        if (timeField == null || timeField.isBlank() || recordDate == null || zone == null) {
            return null;
        }
        LocalTime lt;
        try {
            lt = LocalTime.parse(timeField, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeParseException e) {
            String normalized = timeField;
            if (timeField.length() == 5 && timeField.charAt(2) == ':') {
                normalized = timeField + ":00";
            }
            try {
                lt = LocalTime.parse(normalized, DateTimeFormatter.ofPattern("HH:mm:ss"));
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
        return LocalDateTime.of(recordDate, lt).atZone(zone).toInstant();
    }
}
