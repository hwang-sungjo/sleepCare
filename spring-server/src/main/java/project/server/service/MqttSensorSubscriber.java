package project.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import project.server.dao.HeartRateRepository;
import project.server.dao.RealtimeMetricRepository;
import project.server.dao.entity.RealtimeMetricEntity;
import project.server.entity.HeartRate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 라즈베리파이 MQTT 브로커 구독자.
 *
 * <p>라즈베리파이는 60초 단위로 {@code {"temperature":..,"humidity":..,"lux":..}}
 * 형태의 JSON 을 토픽에 발행한다. 이 컴포넌트는 메시지를 수신할 때마다
 * 다음 동작을 수행한다.</p>
 * <ol>
 *   <li>JSON 을 파싱해 {@code temperature/humidity/illuminance} 를 얻는다.</li>
 *   <li>대상 사용자의 {@code heart_rate} 중 최신 20건을 읽어, <b>KST 현재 날짜·시간(분)</b>과
 *       동일한 row 가 있으면 그 bpm 만 사용하고, 없으면 {@code heart_rate_bpm} 을 null 로 둔다.
 *       (Fitbit 미착용·동기화 전이라 해당 분 데이터가 없으면 null 이 맞다.)</li>
 *   <li>위 값을 합쳐 {@code realtime_metric} 한 행을 insert 한다.</li>
 * </ol>
 *
 * <p>Paho 의 자동 재접속(setAutomaticReconnect=true) 을 사용하여 브로커가 잠시
 * 끊겨도 연결이 자동 복구된다. JPA save 는 MQTT 콜백 스레드에서 호출되며,
 * 호출 자체가 새 트랜잭션을 시작한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttSensorSubscriber {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RealtimeMetricRepository realtimeMetricRepository;
    private final HeartRateRepository heartRateRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.mqtt.broker-url}")
    private String brokerUrl;

    @Value("${app.mqtt.client-id:sleepcare-server}")
    private String clientId;

    @Value("${app.mqtt.topic}")
    private String topic;

    @Value("${app.mqtt.target-user-id:1}")
    private Long targetUserId;

    private MqttClient client;

    @PostConstruct
    public void start() {
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            opts.setConnectionTimeout(10);
            opts.setKeepAliveInterval(60);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("[MQTT] connection lost: {}", cause != null ? cause.getMessage() : "?");
                }

                @Override
                public void messageArrived(String t, MqttMessage message) {
                    handleMessage(message.getPayload());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.connect(opts);
            client.subscribe(topic, 1);
            log.info("[MQTT] subscribed broker={} topic={} userId={}", brokerUrl, topic, targetUserId);
        } catch (Exception e) {
            log.error("[MQTT] failed to start subscriber: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stop() {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (Exception e) {
            log.warn("[MQTT] disconnect error: {}", e.getMessage());
        }
    }

    private void handleMessage(byte[] payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            Double temperature = readDouble(node, "temperature");
            Double humidity = readDouble(node, "humidity");
            Double illuminance = readDouble(node, "lux");

            Integer bpm = resolveBpmForCurrentMinuteAligned(targetUserId);

            RealtimeMetricEntity row = RealtimeMetricEntity.builder()
                    .userId(targetUserId)
                    .illuminance(illuminance)
                    .temperature(temperature)
                    .humidity(humidity)
                    .heartRateBpm(bpm)
                    .build();
            realtimeMetricRepository.save(row);

            log.info("[MQTT] saved realtime_metric userId={} t={} h={} lux={} bpm={}",
                    targetUserId, temperature, humidity, illuminance, bpm);
        } catch (Exception e) {
            log.warn("[MQTT] failed to handle message: {}", e.getMessage());
        }
    }

    private static Double readDouble(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asDouble();
    }

    /**
     * KST 기준 "지금 시각의 날짜 + 시:분" 에 해당하는 heart_rate 행이
     * 최근 20건 안에 있는지 검사한다. 분 단위로만 맞으면 된다 ({@code HH:mm} prefix).
     * 없거나 bpm 이 null 이면 null (실시간 행에는 들어가지 않음).
     */
    private Integer resolveBpmForCurrentMinuteAligned(Long userId) {
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        String minutePrefix = String.format("%02d:%02d", now.getHour(), now.getMinute());

        List<HeartRate> recent = heartRateRepository
                .findTop20ByUserIdOrderByRecordDateDescRecordTimeDesc(userId);
        Optional<HeartRate> match = recent.stream()
                .filter(hr -> today.equals(hr.getRecordDate()))
                .filter(hr -> recordTimeStartsWithMinute(hr.getRecordTime(), minutePrefix))
                .findFirst();

        Integer bpm = match.map(HeartRate::getBpm).orElse(null);
        if (bpm != null && bpm <= 0) {
            return null;
        }
        return bpm;
    }

    private static boolean recordTimeStartsWithMinute(String recordTime, String minutePrefix) {
        if (!StringUtils.hasText(recordTime) || minutePrefix.length() != 5) {
            return false;
        }
        return recordTime.length() >= 5 && recordTime.startsWith(minutePrefix);
    }
}
