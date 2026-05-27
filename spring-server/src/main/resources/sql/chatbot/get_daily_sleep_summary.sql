SELECT
    record_date,
    start_time,
    end_time,
    time_in_bed,
    minutes_asleep,
    minutes_awake,
    efficiency,
    deep_mins,
    light_mins,
    rem_mins,
    wake_mins,
    breathing_rate,
    skin_temp_relative
FROM daily_health_summary
WHERE user_id = :userId
  AND record_date = :recordDate
LIMIT 1
