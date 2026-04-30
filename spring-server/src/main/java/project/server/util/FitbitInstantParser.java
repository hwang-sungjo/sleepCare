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
 * Fitbit API 가 반환하는 시각 문자열을 {@link Instant}로 해석한다.
 */
public final class FitbitInstantParser {

    private static final ZoneId DEFAULT_PARSE_ZONE = ZoneId.of("Asia/Seoul");

    private FitbitInstantParser() {
    }

    /** ISO 오프셋, Zoned, 또는 타임존 없는 로컬 문자열(Asia/Seoul 가정) 등. */
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
     * 심박 intraday 의 {@code time} 필드(예: {@code HH:mm:ss})와 로컬 일자를 합쳐 {@link Instant}로 만든다.
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
