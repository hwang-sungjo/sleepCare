SELECT
    COUNT(*) AS sample_count,
    AVG(bpm) AS avg_bpm,
    MIN(bpm) AS min_bpm,
    MAX(bpm) AS max_bpm
FROM heart_rate
WHERE user_id = :userId
  AND record_date = :recordDate
