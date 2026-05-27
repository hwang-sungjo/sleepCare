SELECT
    'best_efficiency' AS row_type,
    record_date,
    efficiency,
    minutes_asleep,
    deep_mins,
    rem_mins
FROM daily_health_summary
WHERE user_id = :userId
  AND record_date < :recordDate
ORDER BY efficiency DESC, record_date DESC
LIMIT 1
