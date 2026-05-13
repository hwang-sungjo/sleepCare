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
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Fitbit Web API 결과를 로컬 MySQL 각 건강 테이블에 채워 넣는 동기 계층.
 *
 * <p>
 * <b>트리거</b>: 전 사용자 주기 실행 {@link #scheduledFullSync()},
 * 회원별 비동기 {@link #initialSyncForUser(Long)}.
 * </p>
 *
 * <p>
 * 배치 일자축과 심박 증분은 KST 기준 {@link LocalDate} / 분 범위로 맞춘다.
 * Fitbit JSON 시각 문자열은 {@link FitbitInstantParser} 규칙을 따른다(오프셋 없으면 서울 벽시계).
 * </p>
 *
 * <p><b>심박({@link HeartRate})</b></p>
 * <ul>
 * <li><b>7일 플래시:</b> {@link #syncForUser} 에서 과거 포함 일자별 1분 전량을 한 번에 받아 넣으며,
 *     이미 동일 사용자·날짜·분 순간 행이 있으면 insert 생략.</li>
 * <li><b>증분:</b> 매 스케줄 시작 시 직전 {@link #INCREMENTAL_HR_WINDOW_MINUTES} 분만 시간 범위 API 로 당김.</li>
 * </ul>
 *
 * <p>
 * 일일 요약({@link DailyHealthSummary}), 수면 단계, 호흡·체온 등은 같은 날짜 API 체인에서 처리하며,
 * 본수면 레코드의 시작·종료 시각을 파싱할 수 없을 때 해당 일자 블록은 저장하지 않는다.
 * </p>
 *
 * <p>
 * 각 요청 간 {@link #sleep(long)} 호출은 Fitbit 쿼터 상의 연속 과호출 부담을 줄이려는 최소 간격이다.
 * </p>
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

    /** 전체 사용자 토큰 갱신, 심박 증분, 7일치 싱크 순으로 돈다. 시작 시 TTL 기반 realtime_metric 삭제. */
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
        LocalDateTime cutoff = LocalDateTime.now(KST).minus(REALTIME_METRIC_TTL);
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

    /**
     * KST 오늘부터 6일 전까지 순회하여 수면 요약→심박 일괄→SpO2→HRV 를 날마다 처리한다.
     *
     * @param skipExistingDays true 이면 {@code daily_health_summary} 에 이미 해당 날짜가 있으면
     *                         그 날 모든 하위 호출까지 스킵(완료된 날짜 재비용 줄이기).
     */
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

    /**
     * 본수면(main sleep) JSON 이 파싱 가능할 때만 BR·피부온 REST 를 부르고
     * {@link DailyHealthSummary}+{@link SleepStage} 를 한 번에 맞춘다.
     */
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
        Optional<LocalDateTime> startInst = FitbitInstantParser.parseFlexibleLocalDateTime(startRaw);
        Optional<LocalDateTime> endInst = FitbitInstantParser.parseFlexibleLocalDateTime(endRaw);
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
                Optional<LocalDateTime> segStart = FitbitInstantParser.parseFlexibleLocalDateTime(t.path("dateTime").asText(null));
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

    /** {@code sleep} 배열 원소 중에서 {@code isMainSleep}=true 분기 하나를 찾는다; 없으면 null. */
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
    /**
     * 본수면 식별 + 시작/종료 문자열 존재 + {@link project.server.util.FitbitInstantParser} 로
     * 양쪽 모두 순간까지 변환 가능할 때만 true.
     */
    private static boolean isPersistableMainSleep(JsonNode mainSleep) {
        if (mainSleep == null) {
            return false;
        }
        String start = mainSleep.path("startTime").asText(null);
        String end = mainSleep.path("endTime").asText(null);
        if (start == null || start.isBlank() || end == null || end.isBlank()) {
            return false;
        }
        return FitbitInstantParser.parseFlexibleLocalDateTime(start.trim()).isPresent()
                && FitbitInstantParser.parseFlexibleLocalDateTime(end.trim()).isPresent();
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
            LocalDateTime recordTime = FitbitInstantParser.parseHeartRateMinuteToLocalDateTime(recordDate, time, KST);
            if (recordTime == null) {
                continue;
            }
            int bpm = data.path("value").asInt(0);
            if (bpm <= 0) {
                continue;
            }
            if (hrRepo.existsByUserIdAndRecordDateAndRecordTime(userId, recordDate, recordTime)) {
                continue;
            }
            HeartRate row = new HeartRate();
            row.setUserId(userId);
            row.setRecordDate(recordDate);
            row.setRecordTime(recordTime);
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
                    Optional<LocalDateTime> minuteInstant =
                            FitbitInstantParser.parseFlexibleLocalDateTime(data.path("minute").asText(null));
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
                    Optional<LocalDateTime> minuteInstant =
                            FitbitInstantParser.parseFlexibleLocalDateTime(data.path("minute").asText(null));
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
