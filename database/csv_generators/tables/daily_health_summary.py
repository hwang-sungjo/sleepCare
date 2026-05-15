"""daily_health_summary table CSV generator."""

from __future__ import annotations

import csv
import random
from pathlib import Path

from csv_generators.base import TableCsvGenerator
from csv_generators.kst_ranges import (
    iter_wake_dates_last_n_days,
    sleep_session_bounds,
    today_kst_date,
)


def _split_sleep_stages(minutes_asleep: int, rng: random.Random) -> tuple[int, int, int]:
    """Splits asleep minutes into deep, light, and REM with random proportions.

    Args:
        minutes_asleep: Total minutes counted as sleep.
        rng: Random source.

    Returns:
        Tuple ``(deep_mins, light_mins, rem_mins)`` summing to ``minutes_asleep``.

    Raises:
        ValueError: If ``minutes_asleep`` is too small to partition.
    """
    if minutes_asleep < 3:
        raise ValueError("minutes_asleep must be at least 3 for stage split.")

    deep_ratio = rng.uniform(0.14, 0.24)
    rem_ratio = rng.uniform(0.18, 0.28)
    deep_mins = max(1, round(minutes_asleep * deep_ratio))
    rem_mins = max(1, round(minutes_asleep * rem_ratio))
    light_mins = minutes_asleep - deep_mins - rem_mins
    if light_mins < 1:
        shortage = 1 - light_mins
        take_deep = min(shortage, deep_mins - 1)
        deep_mins -= take_deep
        shortage -= take_deep
        rem_mins -= min(shortage, rem_mins - 1)
        light_mins = minutes_asleep - deep_mins - rem_mins
    if light_mins < 1:
        raise ValueError("Could not split sleep stages without violating constraints.")
    return deep_mins, light_mins, rem_mins


class DailyHealthSummaryGenerator(TableCsvGenerator):
    """Generates ``daily_health_summary.csv`` with nightly sleep summaries."""

    table_name = "daily_health_summary"
    fieldnames = (
        "user_id",
        "record_date",
        "start_time",
        "end_time",
        "time_in_bed",
        "minutes_asleep",
        "minutes_awake",
        "efficiency",
        "deep_mins",
        "light_mins",
        "rem_mins",
        "wake_mins",
        "breathing_rate",
        "skin_temp_relative",
        "created_at",
        "updated_at",
    )

    def generate(self, output_dir: Path) -> None:
        """Writes seven rows for nights ending today and previous 6 days (newest row last)."""
        path = self.output_path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        today = today_kst_date()
        rng = random.Random()

        rows: list[dict[str, str]] = []
        for record_date in iter_wake_dates_last_n_days(today, 7):
            start_dt, end_dt = sleep_session_bounds(record_date)
            delta_minutes = int((end_dt - start_dt).total_seconds() // 60)
            if delta_minutes <= 0:
                raise ValueError("Sleep interval must be positive.")

            minutes_awake = rng.randint(32, 78)
            minutes_asleep = delta_minutes - minutes_awake
            if minutes_asleep < 3:
                raise ValueError("minutes_asleep too small; widen awake range or interval.")

            efficiency = round(100 * minutes_asleep / delta_minutes)
            deep_mins, light_mins, rem_mins = _split_sleep_stages(minutes_asleep, rng)
            wake_mins = minutes_awake

            breathing = rng.uniform(11.5, 17.5)
            skin_rel = rng.uniform(-1.2, 1.2)

            rows.append(
                {
                    "user_id": "1",
                    "record_date": record_date.isoformat(),
                    "start_time": start_dt.strftime("%Y-%m-%d %H:%M:%S.000000"),
                    "end_time": end_dt.strftime("%Y-%m-%d %H:%M:%S.000000"),
                    "time_in_bed": str(delta_minutes),
                    "minutes_asleep": str(minutes_asleep),
                    "minutes_awake": str(minutes_awake),
                    "efficiency": str(efficiency),
                    "deep_mins": str(deep_mins),
                    "light_mins": str(light_mins),
                    "rem_mins": str(rem_mins),
                    "wake_mins": str(wake_mins),
                    "breathing_rate": f"{breathing:.1f}",
                    "skin_temp_relative": f"{skin_rel:.1f}",
                    "created_at": "",
                    "updated_at": "",
                },
            )

        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(self.fieldnames))
            writer.writeheader()
            writer.writerows(rows)
