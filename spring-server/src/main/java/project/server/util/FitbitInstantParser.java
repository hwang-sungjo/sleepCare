package project.server.util;

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
 * Fitbit REST JSON 시각 문자열을 <strong>한국 벽시계</strong> {@link LocalDateTime} 으로 통일한다.
 *
 * <p>
 * 오프셋·{@code Z} 가 있으면 그 순간을 {@link #KST} 로 변환한 뒤 로컬 날시를 취한다.
 * 오프셋 없는 로컬 문자열은 이미 KST 벽시계라고 본다.
 * </p>
 */
public final class FitbitInstantParser {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private FitbitInstantParser() {
    }

    /**
     * 수면 시작/종료, SpO2·HRV 분 타임스탬프 등 Fitbit 형식 문자열을 {@link LocalDateTime} (KST) 로 변환한다.
     */
    public static Optional<LocalDateTime> parseFlexibleLocalDateTime(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(text).atZoneSameInstant(KST).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Optional.of(ZonedDateTime.parse(text).withZoneSameInstant(KST).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Optional.of(LocalDateTime.parse(text));
        } catch (DateTimeParseException ignored) {
        }
        return Optional.empty();
    }

    /**
     * 심박 1분 intraday 의 {@code time} 필드를 지정 로컬일과 묶어 {@link LocalDateTime} 으로 만든다.
     * {@code recordDate}·{@code timeField} 는 해당 zone 기준 벽시계로 해석된다.
     */
    public static LocalDateTime parseHeartRateMinuteToLocalDateTime(
            LocalDate recordDate, String timeField, ZoneId zone) {
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
        return LocalDateTime.of(recordDate, lt);
    }
}
