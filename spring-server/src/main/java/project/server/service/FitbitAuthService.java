package project.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import project.server.dao.FitbitRepository;
import project.server.dao.entity.FitbitEntity;

import java.time.Instant;
import java.util.Base64;

/**
 * Fitbit OAuth2 refresh 과정 및 REST 호출 직렬화 헬퍼.
 *
 * <p>
 * {@link #refreshAndPersist(project.server.dao.entity.FitbitEntity)} 는 refresh token 교환 결과를 즉시 DB 에 반영하고
 * 새 access token 문자열을 돌려주며, 토큰 만료 순간은 {@link Instant} 로 저장된다.
 * {@link #callApiAsJson(String, String)} 은 Bearer access 로 GET 결과를 Jackson 트리로 받는 단순 래핑이다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FitbitAuthService {

    /** Fitbit Web API 앱 자격증명. 데모 단계에서 코드에 직접 보관한다. */
    private static final String CLIENT_ID = "23VFLP";
    private static final String CLIENT_SECRET = "aec249d64f0909ed455004ace1ebb5eb";
    private static final String TOKEN_URL = "https://api.fitbit.com/oauth2/token";
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 28_800L;

    private final RestTemplate restTemplate;
    private final FitbitRepository fitbitRepository;

    /**
     * fitbit 행의 refresh token으로 OAuth 토큰 갱신을 수행하고
     * 새로 받은 access/refresh/expires_at 을 같은 행에 즉시 저장한다.
     * 성공 시 새 access token, 실패 시 null.
     */
    public String refreshAndPersist(FitbitEntity entity) {
        String refreshToken = entity.getFitbitRefreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " +
                    Base64.getEncoder().encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes()));

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", refreshToken);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST, new HttpEntity<>(params, headers), JsonNode.class);

            JsonNode body = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && body != null) {
                String newAccess = body.path("access_token").asText(null);
                String newRefresh = body.path("refresh_token").asText(null);
                long expiresIn = body.path("expires_in").asLong(0L);

                if (newAccess == null || newRefresh == null) {
                    return null;
                }

                Instant expiresAt = Instant.now()
                        .plusSeconds(expiresIn > 0 ? expiresIn : DEFAULT_EXPIRES_IN_SECONDS);
                entity.setFitbitAccessToken(newAccess);
                entity.setFitbitRefreshToken(newRefresh);
                entity.setFitbitTokenExpiresAt(expiresAt);
                fitbitRepository.save(entity);

                log.info("[FitbitAuthService] tokens refreshed userId={} expiresAt={}",
                        entity.getUserId(), expiresAt);
                return newAccess;
            }
        } catch (HttpClientErrorException e) {
            log.warn("[FitbitAuthService] token refresh failed ({}): {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("[FitbitAuthService] token refresh error: {}", e.getMessage());
        }
        return null;
    }

    public JsonNode callApiAsJson(String accessToken, String url) {
        if (!StringUtils.hasText(accessToken)) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.warn("[FitbitAuthService] API call failed ({}): {}", e.getStatusCode(), url);
            return null;
        } catch (Exception e) {
            log.warn("[FitbitAuthService] API system error on {}: {}", url, e.getMessage());
            return null;
        }
    }
}
