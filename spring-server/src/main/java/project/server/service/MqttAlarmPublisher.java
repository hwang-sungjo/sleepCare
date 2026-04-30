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
 * 알람 디바이스용 MQTT 명령 발행 클라이언트(QoS 1, retained 미사용).
 *
 * <p>
 * 구독 측은 설정 토픽에서 UTF-8 페이로드 {@code ON} / {@code OFF} 를 수신해 부저·PWM 등 하드웨어를 구동한다.
 * {@link MqttSensorSubscriber} 와 같은 브로커를 쓰되 {@code client-id} 는 별도로 두어 두 연결을 분리한다.
 * </p>
 *
 * <p>
 * 연결 상태 점검을 위해 스케줄에서 ON/OFF 를 주기적으로 번갈아 보내는 경로가 있으며,
 * 비즈니스 알람 발생 시에는 {@link #publish(String)} 으로 단발 제어하면 된다.
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
    /** 현재 순서에 따라 다음에 보낼 명령이 ON 인지 표시한다(토글 발행 전용). */
    private boolean nextCommandIsOn = true;

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
     * 알람 제어 토픽으로 임의 페이로드를 발행한다(연결되어 있지 않으면 무시 후 로그).
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
     * {@code app.mqtt.alarm.toggle-interval-ms} 간격으로 ON 과 OFF 를 번갈아 발행해 구독 측 연동을 검증한다.
     */
    @Scheduled(fixedRateString = "${app.mqtt.alarm.toggle-interval-ms:2000}")
    public void publishToggle() {
        if (client == null || !client.isConnected()) {
            return;
        }
        publish(nextCommandIsOn ? CMD_ON : CMD_OFF);
        nextCommandIsOn = !nextCommandIsOn;
    }
}
