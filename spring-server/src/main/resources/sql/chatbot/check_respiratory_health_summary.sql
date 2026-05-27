SELECT
    record_date,
    breathing_rate,
    minutes_asleep,
    efficiency
FROM daily_health_summary
WHERE user_id = :userId
  AND record_date = :recordDate
LIMIT 1
