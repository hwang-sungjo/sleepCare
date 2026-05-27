SELECT
    'reference_day' AS row_type,
    record_date,
    efficiency,
    minutes_asleep,
    deep_mins,
    rem_mins
FROM daily_health_summary
WHERE user_id = :userId
  AND record_date = :recordDate
LIMIT 1
