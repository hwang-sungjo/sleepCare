package project.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.server.dao.DailyHealthSummaryRepository;
import project.server.dao.HeartRateLogRepository;
import project.server.dao.HrvLogRepository;
import project.server.dao.SleepStageLogRepository;
import project.server.dao.SpO2LogRepository;
import project.server.entity.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class FitbitSyncService {

    private final FitbitAuthService authService;
    private final DailyHealthSummaryRepository summaryRepo;
    private final HeartRateLogRepository hrRepo;
    private final SleepStageLogRepository stageRepo;
    private final SpO2LogRepository spo2Repo;
    private final HrvLogRepository hrvRepo;

    // 🌟 15분마다 실행 (900,000ms). 이전 작업이 끝난 후부터 15분을 셉니다.
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void scheduledFullSync() {
        // 아직 토큰이 입력되지 않은 초기 상태라면 실행하지 않음
        if (authService.getAccessToken() == null || authService.getAccessToken().isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate oldestKeptDate = today.minusDays(6);
        System.out.println("\n🔄 [Scheduled Sync] Starting full 7-days data refresh...");
        purgeOlderThan(oldestKeptDate);

        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            String dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            System.out.println("   ▶ Processing Date: " + dateStr);
            purgeDate(targetDate);

            // 각 API 호출 후 핏빗 서버 보호를 위해 0.5초~1초 정도 짧게 쉽니다.
            syncDailySummaryAndStages(dateStr, targetDate);
            sleep(500);

            syncHeartRate(dateStr, targetDate);
            sleep(500);

            syncSpO2(dateStr, targetDate);
            sleep(500);

            syncHrv(dateStr, targetDate);
            sleep(500);
        }
        System.out.println("✅ [Scheduled Sync] 7-days refresh completed.\n");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void purgeOlderThan(LocalDate cutoffDate) {
        summaryRepo.deleteByRecordDateBefore(cutoffDate);
        hrRepo.deleteByRecordDateBefore(cutoffDate);
        stageRepo.deleteByRecordDateBefore(cutoffDate);
        spo2Repo.deleteByRecordDateBefore(cutoffDate);
        hrvRepo.deleteByRecordDateBefore(cutoffDate);
    }

    private void purgeDate(LocalDate targetDate) {
        summaryRepo.deleteById(targetDate);
        hrRepo.deleteByRecordDate(targetDate);
        stageRepo.deleteByRecordDate(targetDate);
        spo2Repo.deleteByRecordDate(targetDate);
        hrvRepo.deleteByRecordDate(targetDate);
    }

    // --- 이하 데이터 파싱 및 저장 로직 (이전과 동일) ---
    // syncDailySummaryAndStages, syncHeartRate, syncSpO2, syncHrv 구현부...


    private void syncDailySummaryAndStages(String dateStr, LocalDate targetDate) {
        JsonNode sleepNode = authService.callApiAsJson("https://api.fitbit.com/1.2/user/-/sleep/date/" + dateStr + ".json");
        JsonNode brNode = authService.callApiAsJson("https://api.fitbit.com/1/user/-/br/date/" + dateStr + ".json");
        JsonNode tempNode = authService.callApiAsJson("https://api.fitbit.com/1/user/-/temp/skin/date/" + dateStr + ".json");

        DailyHealthSummary summary = new DailyHealthSummary();
        summary.setRecordDate(targetDate);

        // 1. 수면 요약 & 타임라인 파싱
        if (sleepNode != null && sleepNode.has("sleep") && sleepNode.get("sleep").isArray()) {
            for (JsonNode s : sleepNode.get("sleep")) {
                if (s.get("isMainSleep").asBoolean()) {
                    summary.setStartTime(s.get("startTime").asText());
                    summary.setEndTime(s.get("endTime").asText());
                    summary.setTimeInBed(s.get("timeInBed").asInt());
                    summary.setMinutesAsleep(s.get("minutesAsleep").asInt());
                    summary.setMinutesAwake(s.get("minutesAwake").asInt());
                    summary.setEfficiency(s.get("efficiency").asInt());

                    JsonNode stages = s.path("levels").path("summary");
                    if (!stages.isMissingNode()) {
                        summary.setDeepMins(stages.path("deep").path("minutes").asInt(0));
                        summary.setLightMins(stages.path("light").path("minutes").asInt(0));
                        summary.setRemMins(stages.path("rem").path("minutes").asInt(0));
                        summary.setWakeMins(stages.path("wake").path("minutes").asInt(0));
                    }

                    // 수면 타임라인 DB 저장
                    JsonNode timeline = s.path("levels").path("data");
                    if (timeline.isArray()) {
                        for (JsonNode t : timeline) {
                            SleepStageLog log = new SleepStageLog();
                            log.setRecordDate(targetDate);
                            log.setStartTime(t.get("dateTime").asText());
                            log.setDurationSeconds(t.get("seconds").asInt());
                            log.setStageLevel(t.get("level").asText());
                            stageRepo.save(log);
                        }
                    }
                    break;
                }
            }
        }

        // 2. 호흡수 (BR) 파싱
        if (brNode != null && brNode.has("br") && brNode.get("br").isArray() && brNode.get("br").size() > 0) {
            summary.setBreathingRate(brNode.get("br").get(0).path("value").path("breathingRate").asDouble());
        }

        // 3. 피부 온도 (Skin Temp) 파싱
        if (tempNode != null && tempNode.has("tempSkin") && tempNode.get("tempSkin").isArray() && tempNode.get("tempSkin").size() > 0) {
            summary.setSkinTempRelative(tempNode.get("tempSkin").get(0).path("value").path("nightlyRelative").asDouble());
        }

        // 종합 요약 DB 저장
        summaryRepo.save(summary);
    }

    private void syncHeartRate(String dateStr, LocalDate targetDate) {
        JsonNode hrNode = authService.callApiAsJson("https://api.fitbit.com/1/user/-/activities/heart/date/" + dateStr + "/1d/1min.json");
        if (hrNode != null && hrNode.has("activities-heart-intraday")) {
            JsonNode dataset = hrNode.get("activities-heart-intraday").get("dataset");
            if (dataset != null && dataset.isArray()) {
                for (JsonNode data : dataset) {
                    HeartRateLog log = new HeartRateLog();
                    log.setRecordDate(targetDate);
                    log.setRecordTime(data.get("time").asText());
                    log.setBpm(data.get("value").asInt());
                    hrRepo.save(log);
                }
            }
        }
    }

    private void syncSpO2(String dateStr, LocalDate targetDate) {
        JsonNode spo2Node = authService.callApiAsJson("https://api.fitbit.com/1/user/-/spo2/date/" + dateStr + "/all.json");

        // 수정됨: "minuteData"가 아니라 "minutes" 배열을 찾습니다.
        if (spo2Node != null && spo2Node.has("minutes")) {
            JsonNode minutes = spo2Node.get("minutes");
            if (minutes != null && minutes.isArray()) {
                for (JsonNode data : minutes) {
                    SpO2Log log = new SpO2Log();
                    log.setRecordDate(targetDate);
                    // Sandbox 구조: { "minute": "2021-10-01T00:00:00", "value": 96.5 }
                    log.setRecordTime(data.path("minute").asText());
                    log.setSpo2Value(data.path("value").asDouble());
                    spo2Repo.save(log);
                }
            }
        }
    }

    private void syncHrv(String dateStr, LocalDate targetDate) {
        JsonNode hrvNode = authService.callApiAsJson("https://api.fitbit.com/1/user/-/hrv/date/" + dateStr + "/all.json");

        if (hrvNode != null && hrvNode.has("hrv") && hrvNode.get("hrv").isArray() && hrvNode.get("hrv").size() > 0) {
            // 수정됨: "minuteData"가 아니라 "minutes" 배열을 찾습니다.
            JsonNode minutes = hrvNode.get("hrv").get(0).get("minutes");
            if (minutes != null && minutes.isArray()) {
                for (JsonNode data : minutes) {
                    HrvLog log = new HrvLog();
                    log.setRecordDate(targetDate);
                    log.setRecordTime(data.path("minute").asText());
                    // Sandbox 구조: "value" 객체 안에 "rmssd"가 있음
                    log.setRmssdValue(data.path("value").path("rmssd").asDouble());
                    hrvRepo.save(log);
                }
            }
        }
    }
}