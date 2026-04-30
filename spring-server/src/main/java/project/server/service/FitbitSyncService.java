package project.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import project.server.dao.DailyHealthSummaryRepository;
import project.server.dao.FitbitRepository;
import project.server.dao.HeartRateRepository;
import project.server.dao.HrvRepository;
import project.server.dao.RealtimeMetricRepository;
import project.server.dao.SleepStageRepository;
import project.server.dao.SpO2Repository;
import project.server.dao.entity.FitbitEntity;
import project.server.entity.DailyHealthSummary;
import project.server.entity.HeartRate;
import project.server.entity.Hrv;
import project.server.entity.SleepStage;
import project.server.entity.SpO2;
import project.server.util.FitbitInstantParser;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Fitbit Cloud → 로컬 DB 동기화 파이프라인.
 *
 * <p>
 * 두 진입점: {@link #initialSyncForUser(Long)}, {@link #scheduledFullSync()}.
 * </p>
 *
 * <p>
 * 심박({@link HeartRate}) 은 두 갈래이다.
 * </p>
 * <ul>
 * <li><b>일 배치:</b> 7일 루프에서 아직 요약이 없는 날짜에 대해 일 단위 API 로 전체 분 데이터를 한 번 적재 (중복은
 * insert 생략).</li>
 * <li><b>증분:</b> 스케줄마다(기본 15분) KST 기준 직전 {@link #INCREMENTAL_HR_WINDOW_MINUTES}
 * 분 구간을
 * 시간 범위 API 로 가져와, 아직 없는 (user_id, record_date, record_time) 분만 insert.
 * {@code record_time} 은 해당 분의 {@link java.time.Instant} 이다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FitbitSyncService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_HM = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Fitbit 클라우드에 반영되는 심박 지연(~15분)을 고려한 증분 수집 윈도우(분).
     * 직전 20분 데이터만 시간 범위로 조회.
     */
    private static final int INCREMENTAL_HR_WINDOW_MINUTES = 20;

    /** realtime_metric 보존 기한 */
    private static final Duration REALTIME_METRIC_TTL = Duration.ofHours(24);

    private final FitbitAuthService authService;
    private final FitbitRepository fitbitRepository;
    private final DailyHealthSummaryRepository summaryRepo;
    private final HeartRateRepository hrRepo;
    private final SleepStageRepository stageRepo;
    private final SpO2Repository spo2Repo;
    private final HrvRepository hrvRepo;
    private final RealtimeMetricRepository realtimeMetricRepo;

    @Scheduled(fixedDelayString = "${app.fitbit.sync-interval-ms:900000}")
    public void scheduledFullSync() {
        purgeOldRealtimeMetric();
        for (FitbitEntity entity : fitbitRepository.findAll()) {
            String accessToken = authService.refreshAndPersist(entity);
            if (accessToken == null) {
                continue;
            }
            syncIncrementalHeartRateWindow(entity.getUserId(), accessToken);
            syncForUser(entity, accessToken, /* skipExistingDays = */ true);
        }
    }

    /**
     * 보존 기한({@link #REALTIME_METRIC_TTL})을 넘긴 realtime_metric 행을 일괄 삭제한다.
     * 동기화 주기마다 cycle 당 한 번씩만 실행하므로 사용자 수와 무관하게 단일 bulk DELETE 한 번만 발생한다.
     */
    private void purgeOldRealtimeMetric() {
        Instant cutoff = Instant.now().minus(REALTIME_METRIC_TTL);
        int deleted = realtimeMetricRepo.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("[FitbitSyncService] purged {} realtime_metric rows older than {} (TTL={}h)",
                    deleted, cutoff, REALTIME_METRIC_TTL.toHours());
        }
    }

    /**
     * 가입 직후 비동기로 호출되는 초기 적재.
     * refresh로 토큰을 확보·갱신하고 7일치를 존재 여부 확인 없이 그대로 insert 한다.
     */
    @Async
    public void initialSyncForUser(Long userId) {
        FitbitEntity entity = fitbitRepository.findById(userId).orElse(null);
        if (entity == null) {
            log.info("[FitbitSyncService] initial sync skipped userId={} (no fitbit row).", userId);
            return;
        }
        String accessToken = authService.refreshAndPersist(entity);
        if (accessToken == null) {
            log.info("[FitbitSyncService] initial sync skipped userId={} (token invalid).", userId);
            return;
        }
        syncForUser(entity, accessToken, /* skipExistingDays = */ false);
    }

    /**
     * KST 기준 현재부터 직전 {@link #INCREMENTAL_HR_WINDOW_MINUTES} 분 구간의 심박을 Fitbit 시간 범위
     * API 로 가져와
     * 존재하지 않는 분만 DB 에 insert 한다. 자정을 건너는 경우 같은 스케줄에서 최대 두 번 나누어 호출한다.
     */
    private void syncIncrementalHeartRateWindow(long userId, String accessToken) {
        ZonedDateTime end = ZonedDateTime.now(KST).withSecond(0).withNano(0);
        ZonedDateTime start = end.minusMinutes(INCREMENTAL_HR_WINDOW_MINUTES);

        LocalDate startDay = start.toLocalDate();
        LocalDate endDay = end.toLocalDate();
        if (startDay.equals(endDay)) {
            ingestHeartRateIntradayWindow(userId, accessToken, startDay, start.toLocalTime(), end.toLocalTime());
        } else {
            ingestHeartRateIntradayWindow(userId, accessToken, startDay, start.toLocalTime(), LocalTime.of(23, 59));
            ingestHeartRateIntradayWindow(userId, accessToken, endDay, LocalTime.MIDNIGHT, end.toLocalTime());
        }

        log.debug("[FitbitSyncService] incremental HR synced userId={} window={}min (KST)",
                userId, INCREMENTAL_HR_WINDOW_MINUTES);
    }

    /** 단일 로컬일 + [fromTime, toTime] (같은 날 안, end 포함) 시간 범위 API 호출 후 persist. */
    private void ingestHeartRateIntradayWindow(long userId, String accessToken,
            LocalDate date, LocalTime fromTime, LocalTime toTime) {
        if (fromTime.isAfter(toTime)) {
            return;
        }
        String d = date.format(DATE_FMT);
        String url = String.format(
                "https://api.fitbit.com/1/user/-/activities/heart/date/%s/%s/1min/time/%s/%s.json",
                d, d,
                TIME_HM.format(fromTime),
                TIME_HM.format(toTime));
        JsonNode hrNode = authService.callApiAsJson(accessToken, url);
        persistHeartRateDatasetIfAbsent(userId, hrNode, date);
    }

    private void syncForUser(FitbitEntity entity, String accessToken, boolean skipExistingDays) {
        Long userId = entity.getUserId();
        LocalDate today = LocalDate.now(KST);
        log.info("[FitbitSyncService] sync start userId={} window=({} ~ {}) skipExisting={}",
                userId, today.minusDays(6), today, skipExistingDays);

        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            if (skipExistingDays && summaryRepo.existsByUserIdAndRecordDate(userId, targetDate)) {
                continue;
            }
            String dateStr = targetDate.format(DATE_FMT);

            JsonNode sleepNode = authService.callApiAsJson(accessToken,
                    "https://api.fitbit.com/1.2/user/-/sleep/date/" + dateStr + ".json");

            syncDailySummaryAndStages(userId, accessToken, dateStr, targetDate, sleepNode);
            sleep(500);
            syncHeartRateFullDay(userId, accessToken, dateStr, targetDate);
            sleep(500);
            syncSpO2(userId, accessToken, dateStr, targetDate);
            sleep(500);
            syncHrv(userId, accessToken, dateStr, targetDate);
            sleep(500);
        }
        log.info("[FitbitSyncService] sync done userId={}", userId);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /** 일일 요약 + 호흡수/체온 + 수면 단계 타임라인을 적재한다. 본 수면(main sleep) 구간 정보가 불충분하면 저장하지 않는다. */
    private void syncDailySummaryAndStages(Long userId, String accessToken, String dateStr,
            LocalDate targetDate, JsonNode sleepNode) {
        JsonNode mainSleep = extractMainSleep(sleepNode);
        if (!isPersistableMainSleep(mainSleep)) {
            log.debug(
                    "[FitbitSyncService] skip daily_health_summary sleep stages userId={} date={} (no complete main sleep)",
                    userId, targetDate);
            return;
        }
        JsonNode brNode = authService.callApiAsJson(accessToken,
                "https://api.fitbit.com/1/user/-/br/date/" + dateStr + ".json");
        JsonNode tempNode = authService.callApiAsJson(accessToken,
                "https://api.fitbit.com/1/user/-/temp/skin/date/" + dateStr + ".json");

        String startRaw = mainSleep.path("startTime").asText("").trim();
        String endRaw = mainSleep.path("endTime").asText("").trim();
        Optional<Instant> startInst = FitbitInstantParser.parseFlexibleInstant(startRaw);
        Optional<Instant> endInst = FitbitInstantParser.parseFlexibleInstant(endRaw);
        if (startInst.isEmpty() || endInst.isEmpty()) {
            log.debug(
                    "[FitbitSyncService] skip daily_health_summary sleep stages userId={} date={} (unparsable sleep bounds)",
                    userId, targetDate);
            return;
        }

        DailyHealthSummary summary = new DailyHealthSummary();
        summary.setUserId(userId);
        summary.setRecordDate(targetDate);
        summary.setStartTime(startInst.get());
        summary.setEndTime(endInst.get());

        summary.setTimeInBed(mainSleep.path("timeInBed").asInt(0));
        summary.setMinutesAsleep(mainSleep.path("minutesAsleep").asInt(0));
        summary.setMinutesAwake(mainSleep.path("minutesAwake").asInt(0));
        summary.setEfficiency(mainSleep.path("efficiency").asInt(0));

        JsonNode stageSummary = mainSleep.path("levels").path("summary");
        if (!stageSummary.isMissingNode()) {
            summary.setDeepMins(stageSummary.path("deep").path("minutes").asInt(0));
            summary.setLightMins(stageSummary.path("light").path("minutes").asInt(0));
            summary.setRemMins(stageSummary.path("rem").path("minutes").asInt(0));
            summary.setWakeMins(stageSummary.path("wake").path("minutes").asInt(0));
        }

        summary.setBreathingRate(extractBreathingRate(brNode));
        summary.setSkinTempRelative(extractSkinTempRelative(tempNode));

        JsonNode timeline = mainSleep.path("levels").path("data");
        if (timeline.isArray()) {
            for (JsonNode t : timeline) {
                Optional<Instant> segStart = FitbitInstantParser.parseFlexibleInstant(t.path("dateTime").asText(null));
                if (segStart.isEmpty()) {
                    continue;
                }
                SleepStage row = new SleepStage();
                row.setUserId(userId);
                row.setRecordDate(targetDate);
                row.setStartTime(segStart.get());
                row.setDurationSeconds(t.path("seconds").asInt(0));
                row.setStageLevel(t.path("level").asText(null));
                stageRepo.save(row);
            }
        }

        summaryRepo.save(summary);
    }

    /** Fitbit 목록 내 본수면 레코드를 찾거나 null. */
    private static JsonNode extractMainSleep(JsonNode sleepNode) {
        if (sleepNode == null || !sleepNode.has("sleep") || !sleepNode.get("sleep").isArray()) {
            return null;
        }
        for (JsonNode s : sleepNode.get("sleep")) {
            if (s.path("isMainSleep").asBoolean(false)) {
                return s;
            }
        }
        return null;
    }

    private static boolean isPersistableMainSleep(JsonNode mainSleep) {
        if (mainSleep == null) {
            return false;
        }
        String start = mainSleep.path("startTime").asText(null);
        String end = mainSleep.path("endTime").asText(null);
        if (start == null || start.isBlank() || end == null || end.isBlank()) {
            return false;
        }
        return FitbitInstantParser.parseFlexibleInstant(start.trim()).isPresent()
                && FitbitInstantParser.parseFlexibleInstant(end.trim()).isPresent();
    }

    private static double extractBreathingRate(JsonNode brNode) {
        if (brNode != null && brNode.has("br") && brNode.get("br").isArray() && brNode.get("br").size() > 0) {
            return brNode.get("br").get(0).path("value").path("breathingRate").asDouble(0);
        }
        return 0d;
    }

    private static double extractSkinTempRelative(JsonNode tempNode) {
        if (tempNode != null && tempNode.has("tempSkin") && tempNode.get("tempSkin").isArray()
                && tempNode.get("tempSkin").size() > 0) {
            return tempNode.get("tempSkin").get(0).path("value").path("nightlyRelative").asDouble(0);
        }
        return 0d;
    }

    /** 일별 전체 1분 intraday 한 번에 받아 존재하지 않는 행만 insert. */
    private void syncHeartRateFullDay(Long userId, String accessToken, String dateStr, LocalDate targetDate) {
        JsonNode hrNode = authService.callApiAsJson(accessToken,
                "https://api.fitbit.com/1/user/-/activities/heart/date/" + dateStr + "/1d/1min.json");
        persistHeartRateDatasetIfAbsent(userId, hrNode, targetDate);
    }

    private void persistHeartRateDatasetIfAbsent(Long userId, JsonNode hrNode, LocalDate recordDate) {
        if (hrNode == null || !hrNode.has("activities-heart-intraday")) {
            return;
        }
        JsonNode dataset = hrNode.get("activities-heart-intraday").path("dataset");
        if (!dataset.isArray()) {
            return;
        }
        int inserted = 0;
        for (JsonNode data : dataset) {
            String time = data.path("time").asText(null);
            if (time == null) {
                continue;
            }
            Instant recordInstant = FitbitInstantParser.parseHeartRateMinuteToInstant(recordDate, time, KST);
            if (recordInstant == null) {
                continue;
            }
            int bpm = data.path("value").asInt(0);
            if (bpm <= 0) {
                continue;
            }
            if (hrRepo.existsByUserIdAndRecordDateAndRecordTime(userId, recordDate, recordInstant)) {
                continue;
            }
            HeartRate row = new HeartRate();
            row.setUserId(userId);
            row.setRecordDate(recordDate);
            row.setRecordTime(recordInstant);
            row.setBpm(bpm);
            hrRepo.save(row);
            inserted++;
        }
        if (inserted > 0) {
            log.debug("[FitbitSyncService] heart_rate inserts userId={} date={} rows={}",
                    userId, recordDate, inserted);
        }
    }

    /** 분 단위 SpO2 를 적재한다. */
    private void syncSpO2(Long userId, String accessToken, String dateStr, LocalDate targetDate) {
        JsonNode spo2Node = authService.callApiAsJson(accessToken,
                "https://api.fitbit.com/1/user/-/spo2/date/" + dateStr + "/all.json");
        if (spo2Node != null && spo2Node.has("minutes")) {
            JsonNode minutes = spo2Node.get("minutes");
            if (minutes != null && minutes.isArray()) {
                for (JsonNode data : minutes) {
                    Optional<Instant> minuteInstant =
                            FitbitInstantParser.parseFlexibleInstant(data.path("minute").asText(null));
                    if (minuteInstant.isEmpty()) {
                        continue;
                    }
                    SpO2 row = new SpO2();
                    row.setUserId(userId);
                    row.setRecordDate(targetDate);
                    row.setRecordTime(minuteInstant.get());
                    row.setSpo2Value(data.path("value").asDouble(0));
                    spo2Repo.save(row);
                }
            }
        }
    }

    /** 분 단위 HRV(rmssd) 를 적재한다. */
    private void syncHrv(Long userId, String accessToken, String dateStr, LocalDate targetDate) {
        JsonNode hrvNode = authService.callApiAsJson(accessToken,
                "https://api.fitbit.com/1/user/-/hrv/date/" + dateStr + "/all.json");
        if (hrvNode != null && hrvNode.has("hrv") && hrvNode.get("hrv").isArray()
                && hrvNode.get("hrv").size() > 0) {
            JsonNode minutes = hrvNode.get("hrv").get(0).get("minutes");
            if (minutes != null && minutes.isArray()) {
                for (JsonNode data : minutes) {
                    Optional<Instant> minuteInstant =
                            FitbitInstantParser.parseFlexibleInstant(data.path("minute").asText(null));
                    if (minuteInstant.isEmpty()) {
                        continue;
                    }
                    Hrv row = new Hrv();
                    row.setUserId(userId);
                    row.setRecordDate(targetDate);
                    row.setRecordTime(minuteInstant.get());
                    row.setRmssdValue(data.path("value").path("rmssd").asDouble(0));
                    hrvRepo.save(row);
                }
            }
        }
    }
}
