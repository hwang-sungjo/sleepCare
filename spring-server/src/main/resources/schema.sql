CREATE TABLE IF NOT EXISTS `user` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `phone_number` VARCHAR(50),
    `nickname` VARCHAR(50) NOT NULL,
    `profile_image` TEXT,
    `status` VARCHAR(20) DEFAULT 'active',
    `fitbit_user_id` VARCHAR(64),
    `fitbit_access_token` TEXT,
    `fitbit_refresh_token` TEXT,
    `fitbit_token_expires_at` DATETIME(6),
    `fitbit_last_synced_at` DATETIME(6)
);

CREATE TABLE IF NOT EXISTS `sensor_data` (
    `sensor_data_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `device_id` VARCHAR(64),
    `recorded_at` DATETIME(6) NOT NULL,
    `illuminance` DOUBLE,
    `temperature` DOUBLE,
    `humidity` DOUBLE,
    CONSTRAINT `fk_sensor_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `fitbit_data` (
    `fitbit_data_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `logged_at` DATETIME(6) NOT NULL,
    `segment_start` DATETIME(6),
    `segment_end` DATETIME(6),
    `sleep_stage` VARCHAR(32),
    `hrv_ms` DECIMAL(10, 2),
    `resting_hr_bpm` INT,
    `payload_json` LONGTEXT,
    CONSTRAINT `fk_fitbit_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `alarm` (
    `alarm_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `base_wake_time` TIME NOT NULL,
    `dynamic_wake_at` DATETIME(6),
    `adaptive_enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `window_minutes_before` INT NOT NULL DEFAULT 30,
    CONSTRAINT `fk_alarm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `uk_alarm_user` UNIQUE (`user_id`)
);
