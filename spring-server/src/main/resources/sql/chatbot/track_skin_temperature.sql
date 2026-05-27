SELECT
    record_date,
    skin_temp_relative,
    efficiency,
    minutes_asleep
FROM daily_health_summary
WHERE user_id = :userId
  AND record_date >= :startDate
  AND record_date <= :endDate
ORDER BY record_date ASC
LIMIT 500
