SELECT
    'historical_stats' AS row_type,
    COUNT(*) AS sample_days,
    AVG(efficiency) AS avg_efficiency,
    MIN(efficiency) AS min_efficiency,
    MAX(efficiency) AS max_efficiency
FROM daily_health_summary
WHERE user_id = :userId
  AND record_date < :recordDate
