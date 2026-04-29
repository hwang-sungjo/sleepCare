-- =====================================================================
-- SleepCare server schema
-- 1) user                 : 서비스 가입자
-- 2) fitbit               : Fitbit OAuth 토큰 보관 (사용자 1:1)
-- 3) realtime_metric      : 라즈베리파이 등 IoT가 1분 단위로 푸시하는 환경 지표(조도/온습)
-- 4) heart_rate           : 기상 시점에 한 번 수집되는 분 단위 Fitbit 심박
-- 5) hrv                  : 기상 시점에 한 번 수집되는 분 단위 HRV(rmssd)
-- 6) sleep_stage          : 기상 시점에 한 번 수집되는 수면 단계 타임라인
-- 7) spo2                 : 기상 시점에 한 번 수집되는 분 단위 SpO2
-- 8) daily_health_summary : 기상 시점에 한 번 수집되는 일일 수면/호흡/체온 요약
-- 9) alarm                : 요일별 1개씩 운용되는 적응형 알람
-- =====================================================================

CREATE TABLE IF NOT EXISTS `user` (
    `user_id`    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    `password`   VARCHAR(255) NOT NULL,
    `nickname`   VARCHAR(50)  NOT NULL,
    `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `uk_user_nickname` UNIQUE (`nickname`)
);

CREATE TABLE IF NOT EXISTS `fitbit` (
    `user_id`                 BIGINT      NOT NULL PRIMARY KEY,
    `fitbit_access_token`     TEXT,
    `fitbit_refresh_token`    TEXT,
    `fitbit_token_expires_at` DATETIME(6),
    `created_at`              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_fitbit_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `realtime_metric` (
    `realtime_metric_id` BIGINT      AUTO_INCREMENT PRIMARY KEY,
    `user_id`            BIGINT      NOT NULL,
    `illuminance`        DOUBLE,
    `temperature`        DOUBLE,
    `humidity`           DOUBLE,
    `created_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_realtime_metric_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `heart_rate` (
    `id`          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    `user_id`     BIGINT      NOT NULL,
    `record_date` DATE        NOT NULL,
    `record_time` VARCHAR(32),
    `bpm`         INT,
    `created_at`  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_heart_rate_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `hrv` (
    `id`          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    `user_id`     BIGINT      NOT NULL,
    `record_date` DATE        NOT NULL,
    `record_time` VARCHAR(32),
    `rmssd_value` DOUBLE,
    `created_at`  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_hrv_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `sleep_stage` (
    `id`               BIGINT      AUTO_INCREMENT PRIMARY KEY,
    `user_id`          BIGINT      NOT NULL,
    `record_date`      DATE        NOT NULL,
    `start_time`       VARCHAR(64),
    `duration_seconds` INT,
    `stage_level`      VARCHAR(32),
    `created_at`       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_sleep_stage_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `spo2` (
    `id`          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    `user_id`     BIGINT      NOT NULL,
    `record_date` DATE        NOT NULL,
    `record_time` VARCHAR(32),
    `spo2_value`  DOUBLE,
    `created_at`  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_spo2_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `daily_health_summary` (
    `user_id`            BIGINT      NOT NULL,
    `record_date`        DATE        NOT NULL,
    `start_time`         VARCHAR(64),
    `end_time`           VARCHAR(64),
    `time_in_bed`        INT,
    `minutes_asleep`     INT,
    `minutes_awake`      INT,
    `efficiency`         INT,
    `deep_mins`          INT,
    `light_mins`         INT,
    `rem_mins`           INT,
    `wake_mins`          INT,
    `breathing_rate`     DOUBLE,
    `skin_temp_relative` DOUBLE,
    `created_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`user_id`, `record_date`),
    CONSTRAINT `fk_daily_health_summary_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `alarm` (
    `alarm_id`              BIGINT      AUTO_INCREMENT PRIMARY KEY,
    `user_id`               BIGINT      NOT NULL,
    `day_of_week`           TINYINT     NOT NULL,
    `base_wake_time`        TIME        NOT NULL,
    `dynamic_wake_at`       DATETIME(6),
    `adaptive_enabled`      BOOLEAN     NOT NULL DEFAULT TRUE,
    `window_minutes_before` INT         NOT NULL DEFAULT 30,
    `created_at`            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_alarm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `uk_alarm_user_day` UNIQUE (`user_id`, `day_of_week`)
);
