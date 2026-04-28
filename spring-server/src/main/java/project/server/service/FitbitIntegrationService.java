package project.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import project.server.common.exception.UserException;
import project.server.dao.FitbitDataRepository;
import project.server.dao.UserRepository;
import project.server.dao.entity.FitbitDataEntity;
import project.server.dao.entity.UserEntity;
import project.server.dto.fitbit.FitbitOAuthExchangeRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static project.server.common.response.status.BaseExceptionResponseStatus.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class FitbitIntegrationService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FITBIT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final FitbitDataRepository fitbitDataRepository;
    private final DynamicAlarmService dynamicAlarmService;

    @Value("${app.fitbit-client-id}")
    private String fitbitClientId;

    @Value("${app.fitbit-client-secret}")
    private String fitbitClientSecret;

    @Value("${app.fitbit-callback-uri:http://localhost:9000/fitbit/callback}")
    private String fitbitCallbackUri;

    @Transactional
    public void exchangeAuthorizationCode(long userId, FitbitOAuthExchangeRequest request) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new UserException(USER_NOT_FOUND));
        String redirect = Optional.ofNullable(request.getRedirectUri()).filter(s -> !s.isBlank()).orElse(fitbitCallbackUri);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", fitbitClientId);
        form.add("grant_type", "authorization_code");
        form.add("code", request.getCode());
        form.add("redirect_uri", redirect);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(fitbitClientId, fitbitClientSecret);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.USER_AGENT, "SleepCareServer/1.0");

        Map<String, Object> tokenResponse = postForm("https://api.fitbit.com/oauth2/token", headers, form);
        applyTokenResponse(user, tokenResponse);
        userRepository.save(user);
    }

    @Transactional
    public void syncYesterdayForUser(long userId) {
        LocalDate yesterday = LocalDate.now(SEOUL).minusDays(1);
        fetchAndPersistSleep(userId, yesterday);
    }

    @Transactional
    public void fetchAndPersistSleep(long userId, LocalDate night) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new UserException(USER_NOT_FOUND));
        refreshIfStale(user);
        user = userRepository.findById(userId).orElseThrow(() -> new UserException(USER_NOT_FOUND));

        String date = night.format(FITBIT_DATE);
        String url = "https://api.fitbit.com/1.2/user/-/sleep/date/" + date + ".json";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders(user.getFitbitAccessToken())), String.class);
            finalizeSleepSync(userId, response.getBody());
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 401 && user.getFitbitRefreshToken() != null) {
                refreshAccessToken(user);
                userRepository.saveAndFlush(user);
                user = userRepository.findById(userId).orElseThrow(() -> new UserException(USER_NOT_FOUND));
                ResponseEntity<String> retry = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(authHeaders(user.getFitbitAccessToken())), String.class);
                finalizeSleepSync(userId, retry.getBody());
            } else {
                throw ex;
            }
        }
    }

    private void finalizeSleepSync(long userId, String body) {
        persistSleepJson(userId, body, Instant.now());
        userRepository
                .findById(userId)
                .ifPresent(u -> {
                    u.setFitbitLastSyncedAt(LocalDateTime.now());
                    userRepository.save(u);
                });
        dynamicAlarmService.recalculateForUser(userId);
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.USER_AGENT, "SleepCareServer/1.0");
        return headers;
    }

    private void refreshIfStale(UserEntity user) {
        if (needsRefresh(user)) {
            refreshAccessToken(user);
            userRepository.save(user);
        }
    }

    private boolean needsRefresh(UserEntity user) {
        if (user.getFitbitRefreshToken() == null || user.getFitbitRefreshToken().isBlank()) {
            return false;
        }
        Instant expiresAt =
                Optional.ofNullable(user.getFitbitTokenExpiresAt())
                        .map(ldt -> ldt.atZone(ZoneId.systemDefault()).toInstant())
                        .orElse(Instant.EPOCH);
        return expiresAt.isBefore(Instant.now().plus(5, ChronoUnit.MINUTES));
    }

    private void refreshAccessToken(UserEntity user) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", Objects.requireNonNull(user.getFitbitRefreshToken()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(fitbitClientId, fitbitClientSecret);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.USER_AGENT, "SleepCareServer/1.0");

        Map<String, Object> resp = postForm("https://api.fitbit.com/oauth2/token", headers, form);
        applyTokenResponse(user, resp);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForm(String url, HttpHeaders headers, MultiValueMap<String, String> form) {
        ResponseEntity<Map> resp =
                restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
        return resp.getBody();
    }

    private void applyTokenResponse(UserEntity user, Map<String, Object> tokenResponse) {
        String accessToken = (String) tokenResponse.get("access_token");
        Object refresh = tokenResponse.get("refresh_token");
        if (refresh != null) {
            user.setFitbitRefreshToken(String.valueOf(refresh));
        }
        Object fitbitUser = tokenResponse.get("user_id");
        if (fitbitUser != null) {
            user.setFitbitUserId(String.valueOf(fitbitUser));
        }
        Number expiresIn = (Number) tokenResponse.getOrDefault("expires_in", 0);
        user.setFitbitAccessToken(accessToken);
        user.setFitbitTokenExpiresAt(
                LocalDateTime.ofInstant(Instant.now().plusSeconds(expiresIn.longValue()), ZoneId.systemDefault()));
    }

    private void persistSleepJson(long userId, String json, Instant loggedAt) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode sleepArr = root.path("sleep");
            if (!sleepArr.isArray()) {
                return;
            }
            for (JsonNode sleep : sleepArr) {
                appendLevels(userId, sleep.path("levels").path("data"), loggedAt);
                appendLevels(userId, sleep.path("levels").path("shortData"), loggedAt);
                fitbitDataRepository.save(
                        FitbitDataEntity.builder()
                                .userId(userId)
                                .loggedAt(loggedAt)
                                .segmentStart(null)
                                .segmentEnd(null)
                                .sleepStage("session")
                                .hrvMs(null)
                                .restingHrBpm(null)
                                .payloadJson(sleep.toString())
                                .build());
            }
        } catch (Exception e) {
            log.error("[FitbitIntegrationService] failed to parse sleep payload", e);
        }
    }

    private void appendLevels(long userId, JsonNode dataArray, Instant loggedAt) {
        if (!dataArray.isArray()) {
            return;
        }
        for (JsonNode node : dataArray) {
            String dt = node.path("dateTime").asText(null);
            if (dt == null || dt.isBlank()) {
                continue;
            }
            Instant start = Instant.parse(dt);
            String level = node.path("level").asText("unknown");
            fitbitDataRepository.save(
                    FitbitDataEntity.builder()
                            .userId(userId)
                            .loggedAt(loggedAt)
                            .segmentStart(start)
                            .segmentEnd(null)
                            .sleepStage(level)
                            .hrvMs(null)
                            .restingHrBpm(null)
                            .payloadJson(null)
                            .build());
        }
    }
}
