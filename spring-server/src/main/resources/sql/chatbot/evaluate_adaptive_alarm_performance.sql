SELECT
    alarm_id,
    day_of_week,
    base_wake_time,
    dynamic_wake_at,
    adaptive_enabled,
    window_minutes_before,
    updated_at
FROM alarm
WHERE user_id = :userId
ORDER BY day_of_week ASC
LIMIT 20
