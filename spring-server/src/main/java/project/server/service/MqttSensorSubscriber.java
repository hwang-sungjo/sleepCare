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

import project.server.dao.RealtimeMetricRepository;
import project.server.dao.entity.RealtimeMetricEntity;
import project.server.util.FitbitInstantParser;

import java.time.Instant;
import java.util.Optional;

/**
 * 라즈베리파이 MQTT 브로커 구독자.
 *
 * <p>
 * 라즈베리파이는 60초 단위로 {@code {"temperature":..,"humidity":..,"lux":..}}
 * 형태의 JSON 을 토픽에 발행한다. 이 컴포넌트는 메시지를 수신할 때마다
 * 다음 동작을 수행한다.
 * </p>
 * <ol>
 * <li>JSON 을 파싱해 {@code temperature/humidity/illuminance} 를 얻는다.</li>
 * <li>선택 필드 {@code timestamp} / {@code recorded_at} / {@code time}(문자열) 또는 숫자 {@code timestamp} 가 있으면
 * 측정 시각으로 쓴다(문자열에 오프셋이 없으면 <strong>한국(서울) 벽시계</strong>, 숫자는 Unix 초·밀리초 에포크).
 * 없으면 JPA 가 수신 시각을 채운다.</li>
 * <li>위 값을 합쳐 {@code realtime_metric} 한 행을 insert 한다.</li>
 * </ol>
 *
 * <p>
 * Paho 의 자동 재접속(setAutomaticReconnect=true) 을 사용하여 브로커가 잠시
 * 끊겨도 연결이 자동 복구된다. JPA save 는 MQTT 콜백 스레드에서 호출되며,
 * 호출 자체가 새 트랜잭션을 시작한다.
 * </p>
 *
 * <p>
 * 적재되는 {@code realtime_metric.user_id} 는 {@code app.mqtt.target-user-id}(기본 1) 로 고정되며,
 * 동일 값이 {@code user} 에 없으면 외래키 제약 때문에 insert 가 실패한다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttSensorSubscriber {

    private final RealtimeMetricRepository realtimeMetricRepository;
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
        // 센서 JSON → realtime_metric 행으로 변환 저장 (targetUserId 는 구성값 고정).
        try {
            JsonNode node = objectMapper.readTree(payload);
            Double temperature = readDouble(node, "temperature");
            Double humidity = readDouble(node, "humidity");
            Double illuminance = readDouble(node, "lux");

            RealtimeMetricEntity row = RealtimeMetricEntity.builder()
                    .userId(targetUserId)
                    .illuminance(illuminance)
                    .temperature(temperature)
                    .humidity(humidity)
                    .build();
            parseSensorPayloadInstant(node).ifPresent(ts -> {
                row.setCreatedAt(ts);
                row.setUpdatedAt(ts);
            });
            realtimeMetricRepository.save(row);

            log.info("[MQTT] saved realtime_metric userId={} t={} h={} lux={}",
                    targetUserId, temperature, humidity, illuminance);
        } catch (Exception e) {
            log.warn("[MQTT] failed to handle message: {}", e.getMessage());
        }
    }

    /**
     * 라즈베리 JSON 에서 측정 시각을 꺼낸다. 텍스트는 {@link FitbitInstantParser}(오프셋 없으면 서울 벽시계),
     * 정수/실수 {@code timestamp} 는 Unix 초 또는 밀리초로 본다.
     */
    private static Optional<Instant> parseSensorPayloadInstant(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        String[] textKeys = {"timestamp", "recorded_at", "recordedAt", "time"};
        for (String key : textKeys) {
            if (!node.has(key) || node.get(key).isNull()) {
                continue;
            }
            JsonNode v = node.get(key);
            if (v.isTextual()) {
                Optional<Instant> parsed = FitbitInstantParser.parseFlexibleInstant(v.asText());
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
            if (v.isIntegralNumber()) {
                long n = v.asLong();
                return Optional.of(
                        n > 1_000_000_000_000L ? Instant.ofEpochMilli(n) : Instant.ofEpochSecond(n));
            }
        }
        return Optional.empty();
    }

    private static Double readDouble(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asDouble();
    }
}
