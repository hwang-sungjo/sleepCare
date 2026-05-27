SELECT
    AVG(r.illuminance) AS avg_illuminance,
    MAX(r.illuminance) AS max_illuminance,
    MIN(r.illuminance) AS min_illuminance,
    COUNT(*) AS sample_count
FROM realtime_metric r
INNER JOIN daily_health_summary d
    ON d.user_id = r.user_id
   AND d.record_date = :recordDate
WHERE r.user_id = :userId
  AND r.created_at >= d.start_time
  AND r.created_at <= d.end_time
LIMIT 1
