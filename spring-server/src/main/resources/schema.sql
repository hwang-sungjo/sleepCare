CREATE TABLE IF NOT EXISTS `user` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `uk_user_nickname` UNIQUE (`nickname`)
);

CREATE TABLE IF NOT EXISTS `fitbit` (
    `fitbit_user_id` VARCHAR(64) PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `fitbit_user_password` VARCHAR(255) NOT NULL,
    `fitbit_access_token` TEXT,
    `fitbit_refresh_token` TEXT,
    `fitbit_token_expires_at` DATETIME(6),
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_fitbit_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `uk_fitbit_user` UNIQUE (`user_id`)
);

CREATE TABLE IF NOT EXISTS `sensor` (
    `sensor_data_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `illuminance` DOUBLE,
    `temperature` DOUBLE,
    `humidity` DOUBLE,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_sensor_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `fitbit_data` (
    `fitbit_data_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `segment_start` DATETIME(6),
    `segment_end` DATETIME(6),
    `sleep_stage` VARCHAR(32),
    `hrv_ms` DECIMAL(10, 2),
    `resting_hr_bpm` INT,
    `payload_json` LONGTEXT,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_fitbit_data_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `alarm` (
    `alarm_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `base_wake_time` TIME NOT NULL,
    `dynamic_wake_at` DATETIME(6),
    `adaptive_enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `window_minutes_before` INT NOT NULL DEFAULT 30,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_alarm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `uk_alarm_user` UNIQUE (`user_id`)
);
