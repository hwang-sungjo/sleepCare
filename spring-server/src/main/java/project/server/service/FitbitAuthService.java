package project.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
public class FitbitAuthService {
    // 🚨 본인의 Client ID와 Secret으로 변경하세요
    private static final String CLIENT_ID = "23VFLP";
    private static final String CLIENT_SECRET = "eb0ca95286ba7964637c20d8f2073413";

    private String currentAccessToken = "";
    private String currentRefreshToken = "";
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean initializeToken(String initialRefreshToken) {
        this.currentRefreshToken = initialRefreshToken;
        return refreshTokens();
    }

    private boolean refreshTokens() {
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
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                currentAccessToken = response.getBody().get("access_token").asText();
                currentRefreshToken = response.getBody().get("refresh_token").asText();
                System.out.println("\n✅ [NEW ACCESS TOKEN]: " + currentAccessToken);
                System.out.println("✅ [NEW REFRESH TOKEN]: " + currentRefreshToken + "\n");
                return true;
            }
        } catch (Exception e) {
            System.out.println("❌ Token Refresh Failed: " + e.getMessage());
        }
        return false;
    }

    // 401 에러 시 자동 갱신 로직이 포함된 GET 요청 메서드
    public JsonNode callApiAsJson(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + currentAccessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                System.out.println("⚠️ Access Token Expired. Refreshing...");
                if (refreshTokens()) {
                    headers.set("Authorization", "Bearer " + currentAccessToken);
                    HttpEntity<String> retryRequest = new HttpEntity<>(headers);
                    return restTemplate.exchange(url, HttpMethod.GET, retryRequest, JsonNode.class).getBody();
                }
            }
            System.out.println("❌ API Call Failed (" + e.getStatusCode() + "): " + url);
        } catch (Exception e) {
            System.out.println("❌ API System Error: " + e.getMessage());
        }
        return null; // 실패 시 null 반환
    }
    // 🌟 바깥에서 현재 토큰 상태를 확인할 수 있도록 열어주는 메서드
    public String getAccessToken() {
        return this.currentAccessToken;
    }
}