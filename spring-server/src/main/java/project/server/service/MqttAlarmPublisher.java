package project.server.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 라즈베리파이 알람 스피커 제어용 MQTT 발행자.
 *
 * <p>
 * 라즈베리파이 측에서는 {@code iot/alarm/control} 토픽을 구독하면서
 * {@code "ON"} / {@code "OFF"} 페이로드에 따라 PWM 스피커를 켜고 끈다.
 * 이 컴포넌트는 서버 부팅 직후부터 일정 주기({@code app.mqtt.alarm.toggle-interval-ms},
 * 기본 2초)로 두 명령을 번갈아 발행하여 하드웨어 연결을 검증하는
 * <b>임시 데모 트리거</b> 역할을 한다.
 * </p>
 *
 * <p>
 * 실제 운영에서는 {@code @Scheduled} 토글 메서드를 제거하고,
 * 동적 알람 시각 도달 시점에 {@link #publish(String)} 을 한 번씩 호출하는 형태로
 * 교체하면 된다.
 * </p>
 *
 * <p>
 * 센서 구독자({@code MqttSensorSubscriber})와 같은 브로커를 쓰지만 client-id 가
 * 다르므로 두 연결이 서로를 끊지 않는다.
 * </p>
 */
@Slf4j
@Service
public class MqttAlarmPublisher {

    private static final String CMD_ON = "ON";
    private static final String CMD_OFF = "OFF";

    @Value("${app.mqtt.broker-url}")
    private String brokerUrl;

    @Value("${app.mqtt.alarm.client-id:sleepcare-alarm}")
    private String clientId;

    @Value("${app.mqtt.alarm.topic:iot/alarm/control}")
    private String topic;

    private MqttClient client;
    private boolean alarmOn = false;

    @PostConstruct
    public void start() {
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            opts.setConnectionTimeout(10);
            opts.setKeepAliveInterval(60);

            client.connect(opts);
            log.info("[MQTT/Alarm] connected broker={} topic={} clientId={}", brokerUrl, topic, clientId);
        } catch (Exception e) {
            log.error("[MQTT/Alarm] failed to start publisher: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stop() {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.publish(topic, CMD_OFF.getBytes(StandardCharsets.UTF_8), 1, false);
                client.disconnect();
            }
            client.close();
            log.info("[MQTT/Alarm] disconnected and closed.");
        } catch (Exception e) {
            log.warn("[MQTT/Alarm] disconnect error: {}", e.getMessage());
        }
    }

    /**
     * 임의의 명령을 알람 토픽으로 발행한다 (QoS 1, retained=false).
     * 단발 트리거에 사용.
     */
    public void publish(String command) {
        if (client == null || !client.isConnected()) {
            log.warn("[MQTT/Alarm] publish skipped - client not connected.");
            return;
        }
        try {
            client.publish(topic, command.getBytes(StandardCharsets.UTF_8), 1, false);
            log.info("[MQTT/Alarm] published command={}", command);
        } catch (Exception e) {
            log.warn("[MQTT/Alarm] publish failed ({}): {}", command, e.getMessage());
        }
    }

    /**
     * 데모용 토글 발행. {@code app.mqtt.alarm.toggle-interval-ms} 주기로
     * ON ↔ OFF 를 번갈아 보낸다. 운영 전환 시 이 메서드를 제거하면 된다.
     */
    @Scheduled(fixedRateString = "${app.mqtt.alarm.toggle-interval-ms:2000}")
    public void publishToggle() {
        if (client == null || !client.isConnected()) {
            return;
        }
        alarmOn = !alarmOn;
        publish(alarmOn ? CMD_OFF : CMD_OFF);
    }
}
