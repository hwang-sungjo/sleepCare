SELECT
    COUNT(*) AS sample_count,
    AVG(rmssd_value) AS avg_rmssd,
    MIN(rmssd_value) AS min_rmssd,
    MAX(rmssd_value) AS max_rmssd
FROM hrv
WHERE user_id = :userId
  AND record_date = :recordDate
