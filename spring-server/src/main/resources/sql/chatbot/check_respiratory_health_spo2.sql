SELECT
    COUNT(*) AS sample_count,
    AVG(spo2_value) AS avg_spo2,
    MIN(spo2_value) AS min_spo2,
    MAX(spo2_value) AS max_spo2
FROM spo2
WHERE user_id = :userId
  AND record_date = :recordDate
