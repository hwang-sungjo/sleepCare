SELECT
    record_date,
    efficiency,
    minutes_asleep,
    time_in_bed,
    deep_mins,
    light_mins,
    rem_mins,
    wake_mins
FROM daily_health_summary
WHERE user_id = :userId
  AND record_date >= :startDate
  AND record_date <= :endDate
ORDER BY record_date ASC
LIMIT 500
