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
import project.server.dao.SleepStageRepository;
import project.server.dao.SpO2Repository;
import project.server.dao.entity.FitbitEntity;
import project.server.entity.DailyHealthSummary;
import project.server.entity.HeartRate;
import project.server.entity.Hrv;
import project.server.entity.SleepStage;
import project.server.entity.SpO2;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Fitbit Cloud → 로컬 DB 동기화 파이프라인.
 *
 * <p>두 개의 진입점이 있다.</p>
 * <ul>
 *   <li>{@link #initialSyncForUser(Long)} : 가입 직후 비동기로 1회 호출. 토큰을 refresh로
 *       검증·갱신한 뒤 7일치 데이터를 <b>존재 여부 확인 없이</b> 모두 적재한다.</li>
 *   <li>{@link #scheduledFullSync()} : 주기 호출. 모든 fitbit 행을 돌며 토큰을 갱신하고
 *       (user, date) 쌍이 이미 있는 날짜는 통째로 스킵하여 새로운 날짜만 채운다.</li>
 * </ul>
 * 두 경로 모두 시작 시점에 {@link FitbitAuthService#refreshAndPersist(FitbitEntity)} 으로
 * fitbit 행의 access/refresh/expires_at 을 즉시 갱신한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FitbitSyncService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FitbitAuthService authService;
    private final FitbitRepository fitbitRepository;
    private final DailyHealthSummaryRepository summaryRepo;
    private final HeartRateRepository hrRepo;
    private final SleepStageRepository stageRepo;
    private final SpO2Repository spo2Repo;
    private final HrvRepository hrvRepo;

    @Scheduled(fixedDelayString = "${app.fitbit.sync-interval-ms:900000}")
    public void scheduledFullSync() {
        for (FitbitEntity entity : fitbitRepository.findAll()) {
            String accessToken = authService.refreshAndPersist(entity);
            if (accessToken == null) {
                continue;
            }
            syncForUser(entity, accessToken, /* skipExistingDays = */ true);
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
     * 한 사용자에 대해 최근 7일 윈도우를 순회한다.
     * skipExistingDays=true 면 daily_health_summary 가 이미 있는 날짜는 통째로 스킵한다.
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
            syncHeartRate(userId, accessToken, dateStr, targetDate);
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

    /** 일일 요약 + 호흡수/체온 + 수면 단계 타임라인을 적재한다. */
    private void syncDailySummaryAndStages(Long userId, String accessToken, String dateStr,
            LocalDate targetDate, JsonNode sleepNode) {
        JsonNode brNode = authService.callApiAsJson(accessToken,
                "https://api.fitbit.com/1/user/-/br/date/" + dateStr + ".json");
        JsonNode tempNode = authService.callApiAsJson(accessToken,
                "https://api.fitbit.com/1/user/-/temp/skin/date/" + dateStr + ".json");

        DailyHealthSummary summary = new DailyHealthSummary();
        summary.setUserId(userId);
        summary.setRecordDate(targetDate);

        if (sleepNode != null && sleepNode.has("sleep") && sleepNode.get("sleep").isArray()) {
            for (JsonNode s : sleepNode.get("sleep")) {
                if (s.path("isMainSleep").asBoolean(false)) {
                    summary.setStartTime(s.path("startTime").asText(null));
                    summary.setEndTime(s.path("endTime").asText(null));
                    summary.setTimeInBed(s.path("timeInBed").asInt(0));
                    summary.setMinutesAsleep(s.path("minutesAsleep").asInt(0));
                    summary.setMinutesAwake(s.path("minutesAwake").asInt(0));
                    summary.setEfficiency(s.path("efficiency").asInt(0));

                    JsonNode stages = s.path("levels").path("summary");
                    if (!stages.isMissingNode()) {
                        summary.setDeepMins(stages.path("deep").path("minutes").asInt(0));
                        summary.setLightMins(stages.path("light").path("minutes").asInt(0));
                        summary.setRemMins(stages.path("rem").path("minutes").asInt(0));
                        summary.setWakeMins(stages.path("wake").path("minutes").asInt(0));
                    }

                    JsonNode timeline = s.path("levels").path("data");
                    if (timeline.isArray()) {
                        for (JsonNode t : timeline) {
                            SleepStage row = new SleepStage();
                            row.setUserId(userId);
                            row.setRecordDate(targetDate);
                            row.setStartTime(t.path("dateTime").asText(null));
                            row.setDurationSeconds(t.path("seconds").asInt(0));
                            row.setStageLevel(t.path("level").asText(null));
                            stageRepo.save(row);
                        }
                    }
                    break;
                }
            }
        }

        if (brNode != null && brNode.has("br") && brNode.get("br").isArray() && brNode.get("br").size() > 0) {
            summary.setBreathingRate(brNode.get("br").get(0).path("value").path("breathingRate").asDouble(0));
        }

        if (tempNode != null && tempNode.has("tempSkin") && tempNode.get("tempSkin").isArray()
                && tempNode.get("tempSkin").size() > 0) {
            summary.setSkinTempRelative(
                    tempNode.get("tempSkin").get(0).path("value").path("nightlyRelative").asDouble(0));
        }

        summaryRepo.save(summary);
    }

    /** 분 단위 심박을 적재한다. */
    private void syncHeartRate(Long userId, String accessToken, String dateStr, LocalDate targetDate) {
        JsonNode hrNode = authService.callApiAsJson(accessToken,
                "https://api.fitbit.com/1/user/-/activities/heart/date/" + dateStr + "/1d/1min.json");
        if (hrNode != null && hrNode.has("activities-heart-intraday")) {
            JsonNode dataset = hrNode.get("activities-heart-intraday").get("dataset");
            if (dataset != null && dataset.isArray()) {
                for (JsonNode data : dataset) {
                    HeartRate row = new HeartRate();
                    row.setUserId(userId);
                    row.setRecordDate(targetDate);
                    row.setRecordTime(data.path("time").asText(null));
                    row.setBpm(data.path("value").asInt(0));
                    hrRepo.save(row);
                }
            }
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
                    SpO2 row = new SpO2();
                    row.setUserId(userId);
                    row.setRecordDate(targetDate);
                    row.setRecordTime(data.path("minute").asText(null));
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
                    Hrv row = new Hrv();
                    row.setUserId(userId);
                    row.setRecordDate(targetDate);
                    row.setRecordTime(data.path("minute").asText(null));
                    row.setRmssdValue(data.path("value").path("rmssd").asDouble(0));
                    hrvRepo.save(row);
                }
            }
        }
    }
}
