SELECT
    a.day_of_week,
    a.base_wake_time,
    a.dynamic_wake_at,
    a.window_minutes_before,
    a.adaptive_enabled
FROM alarm a
WHERE a.user_id = :userId
  AND a.day_of_week = :dayOfWeek
LIMIT 1
