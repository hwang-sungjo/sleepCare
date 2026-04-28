-- Seed data for local development (schema.sql based)
-- Password is BCrypt for "Password123!".
INSERT INTO `user` (`password`, `nickname`)
VALUES
    ('$2a$10$0JCv6FIAMSkdhv26ecyBV.AO5KuXSvOLa5oI1eS3qHjLNkJyGBhqu', 'sleepy_user'),
    ('$2a$10$0JCv6FIAMSkdhv26ecyBV.AO5KuXSvOLa5oI1eS3qHjLNkJyGBhqu', 'deep_user');

INSERT INTO `fitbit` (`fitbit_user_id`, `user_id`, `fitbit_user_password`, `fitbit_access_token`, `fitbit_refresh_token`, `fitbit_token_expires_at`)
VALUES
    ('fitbit_sleepy_01', 1, 'fitbit_pw_01', 'access_token_sleepy', 'refresh_token_sleepy', DATE_ADD(NOW(6), INTERVAL 1 DAY)),
    ('fitbit_deep_01', 2, 'fitbit_pw_02', 'access_token_deep', 'refresh_token_deep', DATE_ADD(NOW(6), INTERVAL 1 DAY));

INSERT INTO `sensor` (`user_id`, `illuminance`, `temperature`, `humidity`)
VALUES
    (1, 120.5, 23.4, 61.2),
    (1, 85.2, 22.9, 63.7),
    (1, 40.7, 22.1, 66.0),
    (2, 200.0, 24.1, 49.5);

INSERT INTO `fitbit_data` (`user_id`, `segment_start`, `segment_end`, `sleep_stage`, `hrv_ms`, `resting_hr_bpm`, `payload_json`)
VALUES
    (1, DATE_ADD(NOW(6), INTERVAL -120 MINUTE), DATE_ADD(NOW(6), INTERVAL -110 MINUTE), 'deep', 38.50, 58, '{"source":"seed"}'),
    (1, DATE_ADD(NOW(6), INTERVAL -90 MINUTE), DATE_ADD(NOW(6), INTERVAL -80 MINUTE), 'light', 42.10, 57, '{"source":"seed"}'),
    (1, DATE_ADD(NOW(6), INTERVAL -60 MINUTE), DATE_ADD(NOW(6), INTERVAL -50 MINUTE), 'rem', 45.80, 56, '{"source":"seed"}'),
    (2, DATE_ADD(NOW(6), INTERVAL -100 MINUTE), DATE_ADD(NOW(6), INTERVAL -90 MINUTE), 'light', 40.20, 60, '{"source":"seed"}');

-- Weekly alarm configuration: ISO day-of-week 1=Mon ... 7=Sun
INSERT INTO `alarm` (`user_id`, `day_of_week`, `base_wake_time`, `dynamic_wake_at`, `adaptive_enabled`, `window_minutes_before`)
VALUES
    (1, 1, '07:30:00', NULL, TRUE, 30),
    (1, 2, '08:30:00', NULL, TRUE, 30),
    (1, 3, '09:00:00', NULL, TRUE, 30),
    (1, 4, '07:30:00', NULL, TRUE, 30),
    (1, 5, '07:30:00', NULL, TRUE, 30),
    (1, 6, '09:30:00', NULL, TRUE, 30),
    (1, 7, '10:00:00', NULL, TRUE, 30),
    (2, 1, '06:45:00', NULL, TRUE, 20),
    (2, 2, '06:45:00', NULL, TRUE, 20),
    (2, 3, '06:45:00', NULL, TRUE, 20),
    (2, 4, '06:45:00', NULL, TRUE, 20),
    (2, 5, '06:45:00', NULL, TRUE, 20),
    (2, 6, '08:00:00', NULL, TRUE, 20),
    (2, 7, '08:30:00', NULL, TRUE, 20);
