package project.server.sandbox;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class FitbitHrvIntradaySandbox {

    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIyM1ZGTFAiLCJzdWIiOiJENFNMOVQiLCJpc3MiOiJGaXRiaXQiLCJ0eXAiOiJhY2Nlc3NfdG9rZW4iLCJzY29wZXMiOiJyc29jIHJlY2cgcnNldCByaXJuIHJveHkgcm51dCBycHJvIHJzbGUgcmNmIHJhY3QgcmxvYyBycmVzIHJ3ZWkgcmhyIHJ0ZW0iLCJleHAiOjE3NzY0MzIyMzMsImlhdCI6MTc3NjQwMzQzM30.mAHPGVZRBAXB1u3yL9v-YeYoCe1xNhV2DzX8RflfOs4";

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 조회 날짜 설정 (데이터가 확실히 있는 어제나 오늘 날짜)
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // HRV Intraday API URL
        String url = String.format("https://api.fitbit.com/1/user/-/hrv/date/%s/all.json", dateStr);

        // 2. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + ACCESS_TOKEN);
        HttpEntity<String> request = new HttpEntity<>(headers);

        System.out.println("=== [" + dateStr + "] HRV 분 단위 상세 조회 ===");

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("hrv")) {
                List<Map<String, Object>> hrvList = (List<Map<String, Object>>) body.get("hrv");

                if (hrvList.isEmpty()) {
                    System.out.println("결과: 해당 날짜에 기록된 HRV 데이터가 없습니다.");
                    return;
                }

                // hrv[0] 안에 있는 minutes 배열 추출
                Map<String, Object> hrvRoot = hrvList.get(0);
                List<Map<String, Object>> minutes = (List<Map<String, Object>>) hrvRoot.get("minutes");

                System.out.println("총 데이터 포인트: " + minutes.size() + "개");
                System.out.println("----------------------------------------");

                for (Map<String, Object> entry : minutes) {
                    String time = (String) entry.get("minute");

                    // value는 객체 형태임: { "rmssd": 45.2, "coverage": 0.85 }
                    Map<String, Object> valueMap = (Map<String, Object>) entry.get("value");
                    Object rmssd = valueMap.get("rmssd");

                    System.out.printf("[%s] RMSSD: %s ms\n", time, rmssd);
                }

            }
        } catch (HttpClientErrorException e) {
            System.err.println("API 호출 에러: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}