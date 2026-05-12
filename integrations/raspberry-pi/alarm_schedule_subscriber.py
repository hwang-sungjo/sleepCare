#!/usr/bin/env python3
"""
MQTT 로 (1) 일정(JSON wakeAt) 과 (2) 즉시 ON/OFF(문자열) 알람을 처리한다.

- schedule 토픽: 서버가 보내는 다음 기상 시각
    {"type":"wake_schedule","userId":1,"wakeAt":"2026-04-30T22:35:12.345Z"}
- control 토픽: 기존과 동일 ON / OFF (서버 MqttAlarmPublisher 제어용)

설치 (라즈베리파이):
  python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
  MQTT_BROKER=tcp://127.0.0.1:1883 python3 alarm_schedule_subscriber.py

PC 등 GPIO 없음: RPi.GPIO 미설치 시 부저 대신 로그만 출력.
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import sys
import threading
from datetime import datetime, timezone
from typing import Any, Optional

try:
    import paho.mqtt.client as mqtt
except ImportError:
    print("paho-mqtt 가 필요합니다: pip install paho-mqtt", file=sys.stderr)
    sys.exit(1)

try:
    import RPi.GPIO as GPIO
except ImportError:
    GPIO = None  # type: ignore[assignment]

LOGGER = logging.getLogger("alarm_schedule_subscriber")

_WAKE_TYPE = "wake_schedule"
_timer_lock = threading.Lock()
_pending_timer: Optional[threading.Timer] = None

# --- 하드웨어 (제품/배선에 맞게 pin·주파수 조정) ---
_gpio_lock = threading.Lock()
_alarm_playing = False
_pwm_speaker: Any = None
_hardware_ready = False
_speaker_pin: int = 18
_pwm_frequency_hz: int = 440


def parse_broker(broker_url: str) -> tuple[str, int]:
    u = broker_url.strip().rstrip("/")
    for prefix in ("tcp://", "mqtt://"):
        if u.startswith(prefix):
            u = u[len(prefix) :]
            break
    if ":" not in u:
        return (u if u else "127.0.0.1", 1883)
    host, port_s = u.rsplit(":", 1)
    return host.strip(), int(port_s)


def init_speaker_gpio(pin: int, frequency_hz: int) -> bool:
    """
    실제 작동했던 패턴: BCM, 핀 OUT, PWM(기본 440Hz).
    성공 시 True, GPIO 미사용 시 False.
    """
    global _pwm_speaker, _hardware_ready, _speaker_pin, _pwm_frequency_hz

    _speaker_pin = pin
    _pwm_frequency_hz = frequency_hz

    if GPIO is None:
        LOGGER.warning("RPi.GPIO 없음 — 부저는 로그만 (라즈베리파이에서 설치 후 사용)")
        return False

    GPIO.setwarnings(False)
    GPIO.setmode(GPIO.BCM)
    GPIO.setup(pin, GPIO.OUT)
    _pwm_speaker = GPIO.PWM(pin, frequency_hz)
    _hardware_ready = True
    LOGGER.info("GPIO PWM 준비: BCM pin=%s freq=%sHz", pin, frequency_hz)
    return True


def cleanup_speaker_gpio() -> None:
    global _alarm_playing, _pwm_speaker, _hardware_ready

    ring_alarm_stop()
    if _hardware_ready and GPIO is not None:
        try:
            GPIO.cleanup()
        except Exception as e:
            LOGGER.debug("GPIO.cleanup: %s", e)
    _pwm_speaker = None
    _hardware_ready = False


def _parse_wake_instant(s: str) -> datetime:
    s = (s or "").strip()
    if s.endswith("Z"):
        s = s[:-1] + "+00:00"
    return datetime.fromisoformat(s)


def ring_alarm_start() -> None:
    global _alarm_playing

    with _gpio_lock:
        if _alarm_playing:
            return
        LOGGER.warning("알람 시작")
        _alarm_playing = True
        if _hardware_ready and _pwm_speaker is not None:
            try:
                _pwm_speaker.start(1)
            except Exception as e:
                LOGGER.error("PWM start 실패: %s", e)


def ring_alarm_stop() -> None:
    global _alarm_playing

    with _gpio_lock:
        if not _alarm_playing:
            return
        LOGGER.info("알람 중지")
        _alarm_playing = False
        if _hardware_ready and _pwm_speaker is not None:
            try:
                _pwm_speaker.stop()
            except Exception as e:
                LOGGER.debug("PWM stop: %s", e)


def _cancel_pending_timer() -> None:
    global _pending_timer
    with _timer_lock:
        if _pending_timer is not None:
            _pending_timer.cancel()
            _pending_timer = None


def _schedule_ring_at(wake_dt_utc: datetime, jitter_sec: float, ring_secs: float) -> None:
    def _fire() -> None:
        ring_alarm_start()
        if ring_secs > 0:
            t = threading.Timer(ring_secs, ring_alarm_stop)
            t.daemon = True
            t.start()

    global _pending_timer

    now = datetime.now(timezone.utc)
    if wake_dt_utc.tzinfo is None:
        wake_dt_utc = wake_dt_utc.replace(tzinfo=timezone.utc)
    delta = (wake_dt_utc - now).total_seconds()

    _cancel_pending_timer()

    if delta < -jitter_sec:
        LOGGER.info("wakeAt in the past — skip (%s)", wake_dt_utc.isoformat())
        return
    if delta <= jitter_sec:
        LOGGER.info("wakeAt imminent or past → ring now")
        threading.Thread(target=_fire, daemon=True).start()
        return

    LOGGER.info(
        "scheduled ring in %.1fs (wake=%s utc now=%s)",
        delta,
        wake_dt_utc.isoformat(),
        now.isoformat(),
    )

    with _timer_lock:
        _pending_timer = threading.Timer(delta, _fire)
        _pending_timer.daemon = True
        _pending_timer.start()


def _handle_schedule_payload(data: dict[str, Any], jitter_sec: float, ring_secs: float) -> None:
    if data.get("type") != _WAKE_TYPE:
        LOGGER.debug("ignore type=%s", data.get("type"))
        return
    raw = data.get("wakeAt")
    if raw is None:
        LOGGER.warning("missing wakeAt: %s", data)
        return
    wake = _parse_wake_instant(str(raw))
    LOGGER.info("received wake_schedule userId=%s wakeAt=%s", data.get("userId"), raw)
    _schedule_ring_at(wake, jitter_sec=jitter_sec, ring_secs=ring_secs)


def _make_mqtt_client_v2(client_id: str) -> mqtt.Client:
    return mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=client_id)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="MQTT: wake_schedule(JSON) + control ON/OFF, optional PWM speaker on Pi."
    )
    parser.add_argument(
        "--broker",
        default=os.environ.get("MQTT_BROKER", "tcp://127.0.0.1:1883"),
        help="MQTT broker (예: tcp://127.0.0.1:1883)",
    )
    parser.add_argument(
        "--topic",
        default=os.environ.get("MQTT_SCHEDULE_TOPIC", "iot/alarm/schedule"),
        help="일정 토픽 (JSON wake_schedule)",
    )
    parser.add_argument(
        "--control-topic",
        default=os.environ.get("MQTT_CONTROL_TOPIC", "iot/alarm/control"),
        help="문자열 ON/OFF 제어 토픽",
    )
    parser.add_argument(
        "--client-id",
        default=os.environ.get("MQTT_PI_CLIENT_ID", "sleepcare-pi-alarm"),
    )
    parser.add_argument(
        "--speaker-pin",
        type=int,
        default=int(os.environ.get("SPEAKER_PIN", "18")),
        help="BCM 핀 (Zero Speaker PWM 등, 제품에 따라 조정)",
    )
    parser.add_argument(
        "--pwm-freq",
        type=int,
        default=int(os.environ.get("SPEAKER_PWM_HZ", "440")),
        help="PWM 주파수 Hz (기본 440)",
    )
    parser.add_argument(
        "--no-hardware",
        action="store_true",
        help="GPIO 초기화 생략 (개발 PC)",
    )
    parser.add_argument(
        "--jitter-sec",
        type=float,
        default=float(os.environ.get("WAKE_JITTER_SEC", "2")),
    )
    parser.add_argument(
        "--ring-sec",
        type=float,
        default=float(os.environ.get("RING_DURATION_SEC", "30")),
        help="예약 울림 후 자동 OFF 초 (0이면 수동 OFF까지)",
    )
    parser.add_argument("-v", "--verbose", action="store_true")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )

    if not args.no_hardware:
        init_speaker_gpio(args.speaker_pin, args.pwm_freq)

    mqtt_userdata: dict[str, Any] = {
        "schedule_topic": args.topic,
        "control_topic": args.control_topic,
        "jitter_sec": args.jitter_sec,
        "ring_sec": args.ring_sec,
    }

    def on_connect(
        client: mqtt.Client,
        userdata: dict[str, Any],
        flags: mqtt.ConnectFlags,
        reason_code: mqtt.ReasonCode,
        properties: Any,
    ) -> None:
        if reason_code.is_failure:
            LOGGER.error("MQTT 연결 실패: %s", reason_code)
            return
        LOGGER.info("MQTT 연결됨")
        client.subscribe(userdata["schedule_topic"], qos=1)
        client.subscribe(userdata["control_topic"], qos=1)
        LOGGER.info("구독: %s, %s", userdata["schedule_topic"], userdata["control_topic"])

    def on_message(client: mqtt.Client, userdata: dict[str, Any], msg: mqtt.MQTTMessage) -> None:
        sched = userdata["schedule_topic"]
        ctl = userdata["control_topic"]

        if msg.topic == ctl:
            command = msg.payload.decode("utf-8").strip().upper()
            LOGGER.info("[%s] %s", msg.topic, command)
            if command == "ON":
                ring_alarm_start()
            elif command == "OFF":
                ring_alarm_stop()
            return

        if msg.topic != sched:
            LOGGER.debug("unknown topic %s", msg.topic)
            return

        try:
            payload = json.loads(msg.payload.decode("utf-8"))
            if isinstance(payload, dict):
                _handle_schedule_payload(
                    payload,
                    jitter_sec=userdata["jitter_sec"],
                    ring_secs=userdata["ring_sec"],
                )
            else:
                LOGGER.warning("expected JSON object, got %s", type(payload))
        except json.JSONDecodeError as e:
            LOGGER.warning("invalid JSON on schedule topic: %s", e)

    client = _make_mqtt_client_v2(args.client_id)
    client.user_data_set(mqtt_userdata)
    client.on_connect = on_connect
    client.on_message = on_message

    host, port = parse_broker(args.broker)
    LOGGER.info("브로커 %s:%s 대기 중...", host, port)

    try:
        client.connect(host, port, keepalive=60)
        client.loop_forever()
    except KeyboardInterrupt:
        LOGGER.info("종료 요청")
    finally:
        cleanup_speaker_gpio()


if __name__ == "__main__":
    main()
