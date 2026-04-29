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
 * <li>위 값을 합쳐 {@code realtime_metric} 한 행을 insert 한다.</li>
 * </ol>
 *
 * <p>
 * Paho 의 자동 재접속(setAutomaticReconnect=true) 을 사용하여 브로커가 잠시
 * 끊겨도 연결이 자동 복구된다. JPA save 는 MQTT 콜백 스레드에서 호출되며,
 * 호출 자체가 새 트랜잭션을 시작한다.
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
            realtimeMetricRepository.save(row);

            log.info("[MQTT] saved realtime_metric userId={} t={} h={} lux={}",
                    targetUserId, temperature, humidity, illuminance);
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
}
