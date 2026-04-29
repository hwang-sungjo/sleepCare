package project.server.sandbox;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class FitbitAutoTokenMonitor {

    private static final String CLIENT_ID = "23VFLP";
    private static final String CLIENT_SECRET = "eb0ca95286ba7964637c20d8f2073413";

    private static String currentAccessToken = "";
    private static String currentRefreshToken = "";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RestTemplate restTemplate = new RestTemplate();

        System.out.print("Enter your INITIAL Refresh Token: ");
        currentRefreshToken = scanner.nextLine();

        if (!refreshTokens(restTemplate)) {
            System.out.println("SYSTEM_ERROR: Initial token refresh failed.");
            return;
        }

        try {
            while (true) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                System.out.println("\n---FETCH_START---");
                System.out.println("TIMESTAMP: " + timestamp);

                // =========================================================
                // [1] 최근 데이터 포인트 가져오기 (시간|값 형태)
                // =========================================================

                // 심장/혈액 관련 (HR, SpO2, HRV) - 분 단위 15개
                printRecentIntervalData(restTemplate, "HR_15", "https://api.fitbit.com/1/user/-/activities/heart/date/today/1d/1min.json", "activities-heart-intraday", "dataset", 15);
                printRecentIntervalData(restTemplate, "SPO2_15", "https://api.fitbit.com/1/user/-/spo2/date/today/all.json", "spo2", "minutes", 15);
                printRecentIntervalData(restTemplate, "HRV_15", "https://api.fitbit.com/1/user/-/hrv/date/today/all.json", "hrv", "minutes", 15);

                // 활동량 관련 (Steps, Calories, Distance, Floors) - 분 단위 15개
                printRecentIntervalData(restTemplate, "STEPS_15", "https://api.fitbit.com/1/user/-/activities/steps/date/today/1d/1min.json", "activities-steps-intraday", "dataset", 15);
                printRecentIntervalData(restTemplate, "CALS_15", "https://api.fitbit.com/1/user/-/activities/calories/date/today/1d/1min.json", "activities-calories-intraday", "dataset", 15);
                printRecentIntervalData(restTemplate, "DIST_15", "https://api.fitbit.com/1/user/-/activities/distance/date/today/1d/1min.json", "activities-distance-intraday", "dataset", 15);
                printRecentIntervalData(restTemplate, "FLOORS_15", "https://api.fitbit.com/1/user/-/activities/floors/date/today/1d/1min.json", "activities-floors-intraday", "dataset", 15);

                // 야간 전용 건강 지표 (Breathing Rate, Skin Temperature) - 하루 1개 요약
                printRecentIntervalData(restTemplate, "BR_1", "https://api.fitbit.com/1/user/-/br/date/today/today.json", "br", null, 1);
                printRecentIntervalData(restTemplate, "SKIN_TEMP_1", "https://api.fitbit.com/1/user/-/temp/skin/date/today/today.json", "tempSkin", null, 1);

                // =========================================================
                // [2] 수면 데이터 파싱 (상태 및 요약)
                // =========================================================

                // 오늘 수면 데이터에서 최근 15개 상태 가져오기 (시간|상태 형태)
                Map<String, Object> todaySleep = fetchMainSleep(restTemplate, LocalDate.now());
                printRecentSleepStates(todaySleep, 15);

                // 수면 데이터 요약 (어제, 오늘, 내일)
                LocalDate today = LocalDate.now();
                printDailySleepSummary(restTemplate, "YESTERDAY", today.minusDays(1));
                printDailySleepSummary(restTemplate, "TODAY", today);
                printDailySleepSummary(restTemplate, "TOMORROW", today.plusDays(1));

                System.out.println("---FETCH_END---");

                Thread.sleep(15 * 60 * 1000); // 15분 대기
            }
        } catch (InterruptedException e) {
            System.out.println("SYSTEM_MSG: Monitoring stopped.");
        } finally {
            scanner.close();
        }
    }

    // ====================================================================
    // --- [토큰 갱신 및 API 호출 자동화 로직] ---
    // ====================================================================
    private static boolean refreshTokens(RestTemplate restTemplate) {
        try {
            String url = "https://api.fitbit.com/oauth2/token";
            String authStr = CLIENT_ID + ":" + CLIENT_SECRET;
            String base64Auth = Base64.getEncoder().encodeToString(authStr.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + base64Auth);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", currentRefreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                currentAccessToken = (String) response.getBody().get("access_token");
                currentRefreshToken = (String) response.getBody().get("refresh_token");

                System.out.println("SYSTEM_MSG: TOKEN_REFRESH_SUCCESS");
                System.out.println("NEW_ACCESS_TOKEN: " + currentAccessToken);
                System.out.println("NEW_REFRESH_TOKEN: " + currentRefreshToken);
                return true;
            }
        } catch (Exception e) {
            System.out.println("SYSTEM_ERROR: TOKEN_REFRESH_FAILED");
        }
        return false;
    }

    private static Map<String, Object> callApiWithRetry(RestTemplate restTemplate, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + currentAccessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                System.out.println("SYSTEM_MSG: API_401_AUTO_REFRESHING");
                if (refreshTokens(restTemplate)) {
                    headers.set("Authorization", "Bearer " + currentAccessToken);
                    HttpEntity<String> retryRequest = new HttpEntity<>(headers);
                    try {
                        return restTemplate.exchange(url, HttpMethod.GET, retryRequest, Map.class).getBody();
                    } catch (Exception retryEx) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            // 조용한 실패 처리
        }
        return null;
    }

    // ====================================================================
    // --- [데이터 파싱 및 출력 Helper Methods] ---
    // ====================================================================

    private static Map<String, Object> fetchMainSleep(RestTemplate restTemplate, LocalDate targetDate) {
        String dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String url = "https://api.fitbit.com/1.2/user/-/sleep/date/" + dateStr + ".json";

        Map<String, Object> responseBody = callApiWithRetry(restTemplate, url);

        if (responseBody != null) {
            List<Map<String, Object>> sleepList = (List<Map<String, Object>>) responseBody.get("sleep");
            if (sleepList != null) {
                for (Map<String, Object> sleepObj : sleepList) {
                    if (Boolean.TRUE.equals(sleepObj.get("isMainSleep"))) {
                        return sleepObj;
                    }
                }
            }
        }
        return null;
    }

    private static void printRecentIntervalData(RestTemplate restTemplate, String key, String url, String rootKey, String arrayKey, int count) {
        Map<String, Object> responseBody = callApiWithRetry(restTemplate, url);

        if (responseBody != null) {
            try {
                List<Map<String, Object>> dataList = null;

                // API별 JSON 구조 차이 완벽 대응
                if (rootKey != null && rootKey.contains("activities-") && rootKey.contains("-intraday")) {
                    Map<String, Object> root = (Map<String, Object>) responseBody.get(rootKey);
                    if (root != null) dataList = (List<Map<String, Object>>) root.get(arrayKey);

                } else if ("hrv".equals(rootKey) || "spo2".equals(rootKey)) {
                    // HRV, SpO2 (분 단위)
                    List<Map<String, Object>> rootList = (List<Map<String, Object>>) responseBody.get(rootKey);
                    if (rootList != null && !rootList.isEmpty() && arrayKey != null) {
                        dataList = (List<Map<String, Object>>) rootList.get(0).get(arrayKey);
                    }

                } else if ("br".equals(rootKey) || "tempSkin".equals(rootKey)) {
                    // 호흡수, 피부 온도 (일 단위)
                    dataList = (List<Map<String, Object>>) responseBody.get(rootKey);

                } else {
                    dataList = (List<Map<String, Object>>) responseBody.get(arrayKey);
                }

                if (dataList != null && !dataList.isEmpty()) {
                    int startIndex = Math.max(0, dataList.size() - count);
                    List<Map<String, Object>> recentData = dataList.subList(startIndex, dataList.size());

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < recentData.size(); i++) {
                        Map<String, Object> dp = recentData.get(i);

                        // 시간 정보 추출
                        String timeStr = "unknown";
                        if (dp.containsKey("time")) timeStr = String.valueOf(dp.get("time"));
                        else if (dp.containsKey("minute")) timeStr = String.valueOf(dp.get("minute"));
                        else if (dp.containsKey("dateTime")) timeStr = String.valueOf(dp.get("dateTime"));

                        // 값 정보 추출 (BR 객체 꼬임 문제 포함)
                        Object val = "null";
                        if (dp.containsKey("value")) {
                            Object v = dp.get("value");
                            if (v instanceof Map) {
                                // BR(호흡수)의 경우 value 내부에 { "breathingRate": 14.2 } 형태로 존재
                                Map<?, ?> mapV = (Map<?, ?>) v;
                                val = mapV.containsKey("breathingRate") ? mapV.get("breathingRate") : v;
                            } else {
                                val = v;
                            }
                        } else if (dp.containsKey("rmssd")) {
                            val = dp.get("rmssd"); // HRV
                        }

                        // "시간|값" 포맷으로 저장
                        sb.append(timeStr).append("|").append(val);
                        if (i < recentData.size() - 1) sb.append(",");
                    }
                    System.out.println(key + ": " + sb.toString());
                    return;
                }
            } catch (Exception e) {
                // 파싱 에러 시 아래 null 출력으로 빠짐
            }
        }
        System.out.println(key + ": null");
    }

    private static void printRecentSleepStates(Map<String, Object> sleepData, int count) {
        if (sleepData == null) {
            System.out.println("SLEEP_STATES_15: null");
            return;
        }

        try {
            Map<String, Object> levels = (Map<String, Object>) sleepData.get("levels");
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) levels.get("data");

            if (dataList != null && !dataList.isEmpty()) {
                int startIndex = Math.max(0, dataList.size() - count);
                List<Map<String, Object>> recentData = dataList.subList(startIndex, dataList.size());

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < recentData.size(); i++) {
                    Map<String, Object> stage = recentData.get(i);

                    // 수면 상태 시작 시간 추출
                    String dateTime = stage.containsKey("dateTime") ? String.valueOf(stage.get("dateTime")) : "unknown";

                    // "시간|상태(초)" 포맷으로 저장
                    sb.append(dateTime).append("|").append(stage.get("level")).append("(").append(stage.get("seconds")).append(")");
                    if (i < recentData.size() - 1) sb.append(",");
                }
                System.out.println("SLEEP_STATES_15: " + sb.toString());
                return;
            }
        } catch (Exception e) {
            // 파싱 에러
        }
        System.out.println("SLEEP_STATES_15: null");
    }

    private static void printDailySleepSummary(RestTemplate restTemplate, String dayLabel, LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String prefix = dayLabel + "_" + dateStr + "_";

        Map<String, Object> sleepData = fetchMainSleep(restTemplate, date);

        if (sleepData == null) {
            System.out.println(prefix + "startTime: null");
            System.out.println(prefix + "endTime: null");
            System.out.println(prefix + "timeInBed: null");
            System.out.println(prefix + "minutesAsleep: null");
            System.out.println(prefix + "minutesAwake: null");
            System.out.println(prefix + "efficiency: null");
            System.out.println(prefix + "Wake: null");
            System.out.println(prefix + "Light: null");
            System.out.println(prefix + "Deep: null");
            System.out.println(prefix + "Rem: null");
            return;
        }

        System.out.println(prefix + "startTime: " + sleepData.getOrDefault("startTime", "null"));
        System.out.println(prefix + "endTime: " + sleepData.getOrDefault("endTime", "null"));
        System.out.println(prefix + "timeInBed: " + sleepData.getOrDefault("timeInBed", "null"));
        System.out.println(prefix + "minutesAsleep: " + sleepData.getOrDefault("minutesAsleep", "null"));
        System.out.println(prefix + "minutesAwake: " + sleepData.getOrDefault("minutesAwake", "null"));
        System.out.println(prefix + "efficiency: " + sleepData.getOrDefault("efficiency", "null"));

        Map<String, Object> levels = (Map<String, Object>) sleepData.get("levels");
        Map<String, Object> summary = (levels != null && levels.containsKey("summary")) ? (Map<String, Object>) levels.get("summary") : null;

        System.out.println(prefix + "Wake: " + getStageMinutes(summary, "wake"));
        System.out.println(prefix + "Light: " + getStageMinutes(summary, "light"));
        System.out.println(prefix + "Deep: " + getStageMinutes(summary, "deep"));
        System.out.println(prefix + "Rem: " + getStageMinutes(summary, "rem"));
    }

    private static String getStageMinutes(Map<String, Object> summary, String stage) {
        if (summary != null && summary.containsKey(stage)) {
            Map<String, Object> details = (Map<String, Object>) summary.get(stage);
            return String.valueOf(details.get("minutes"));
        }
        return "null";
    }
}