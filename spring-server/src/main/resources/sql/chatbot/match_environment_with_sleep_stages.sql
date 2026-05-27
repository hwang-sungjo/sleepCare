SELECT
    s.stage_level,
    SUM(s.duration_seconds) AS total_duration_seconds,
    COUNT(*) AS segment_count
FROM sleep_stage s
INNER JOIN daily_health_summary d
    ON d.user_id = s.user_id
   AND d.record_date = s.record_date
WHERE s.user_id = :userId
  AND s.record_date = :recordDate
GROUP BY s.stage_level
ORDER BY total_duration_seconds DESC
LIMIT 50
