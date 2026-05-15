package project.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 라즈베리파이 알람 장치용 MQTT 클라이언트.
 *
 * <p>
 * 제어 토픽({@link #topic}): {@code ON} / {@code OFF} 로 즉시 부저 제어.
 * 일정 토픽({@link #scheduleTopic}): JSON 으로 다음 기상 시각 전달 — 파이 측에서 해당 시각에 울림.
 * </p>
 */
@Slf4j
@Profile("!lambda")
@Service
public class MqttAlarmPublisher {

    private static final String CMD_ON = "ON";
    private static final String CMD_OFF = "OFF";

    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Seoul");

    /** Pi 쪽 {@code datetime.fromisoformat} 호환을 위해 한국 오프셋이 붙은 ISO 문자열로 직렬화. */
    private static final DateTimeFormatter WAKE_AT_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.mqtt.broker-url}")
    private String brokerUrl;

    @Value("${app.mqtt.alarm.client-id:sleepcare-alarm}")
    private String clientId;

    @Value("${app.mqtt.alarm.topic:iot/alarm/control}")
    private String topic;

    /**
     * 서버가 계산한 다음 알람 시각을 라즈베리 구독 스크립트에게 넘기는 전용 토픽.
     */
    @Value("${app.mqtt.alarm.schedule-topic:iot/alarm/schedule}")
    private String scheduleTopic;

    /**
     * false 이면 {@link #publishWakeSchedule(long, LocalDateTime)} 가 아무 것도 보내지 않는다 (브로커 테스트 등).
     */
    @Value("${app.mqtt.alarm.publish-schedule-to-raspberry:true}")
    private boolean publishScheduleToRaspberry;

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

            client.connect(opts);
            log.info("[MQTT/Alarm] connected broker={} controlTopic={} scheduleTopic={} clientId={}",
                    brokerUrl, topic, scheduleTopic, clientId);
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
     * 알람 제어 토픽으로 즉시 문자열 명령 발행.
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
     * 라즈베리파이가 구독하는 일정 토픽으로 다음 기상 시각(JSON)을 보낸다.
     *
     * <p>
     * 페이로드 예시: {@code {"type":"wake_schedule","userId":1,"wakeAt":"2026-04-30T07:30:00+09:00"}}
     * {@code wakeAt} 은 서버가 한국(Asia/Seoul) 벽시계로 보관한 시각을 오프셋 포함 ISO-8601 로 표현한 값이다.
     * </p>
     */
    public void publishWakeSchedule(long userId, LocalDateTime wakeAt) {
        if (!publishScheduleToRaspberry) {
            return;
        }
        if (wakeAt == null) {
            log.warn("[MQTT/Alarm] publishWakeSchedule skipped — wakeAt null");
            return;
        }
        if (client == null || !client.isConnected()) {
            log.warn("[MQTT/Alarm] schedule publish skipped — client not connected");
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "wake_schedule");
            payload.put("userId", userId);
            String wakeAtStr = wakeAt.atZone(SCHEDULE_ZONE).format(WAKE_AT_FORMAT);
            payload.put("wakeAt", wakeAtStr);
            byte[] body = objectMapper.writeValueAsBytes(payload);
            client.publish(scheduleTopic, body, 1, false);
            log.info("[MQTT/Alarm] published wake_schedule user={} wakeAt={} topic={}",
                    userId, wakeAtStr, scheduleTopic);
        } catch (Exception e) {
            log.warn("[MQTT/Alarm] schedule publish failed user={}: {}", userId, e.getMessage());
        }
    }
}
