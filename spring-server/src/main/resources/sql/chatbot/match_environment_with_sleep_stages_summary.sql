SELECT
    d.record_date,
    d.start_time AS sleep_start,
    d.end_time AS sleep_end,
    AVG(r.temperature) AS avg_temperature,
    AVG(r.humidity) AS avg_humidity,
    AVG(r.illuminance) AS avg_illuminance,
    COUNT(r.realtime_metric_id) AS env_sample_count
FROM daily_health_summary d
LEFT JOIN realtime_metric r
    ON r.user_id = d.user_id
   AND r.created_at >= d.start_time
   AND r.created_at <= d.end_time
WHERE d.user_id = :userId
  AND d.record_date = :recordDate
GROUP BY d.record_date, d.start_time, d.end_time
LIMIT 1
