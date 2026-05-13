"""hrv table CSV generator."""

from __future__ import annotations

import csv
import random
from datetime import timedelta
from pathlib import Path

from csv_generators.base import TableCsvGenerator
from csv_generators.kst_ranges import iter_wake_dates_last_n_days, sleep_session_bounds, today_kst_date


class HrvGenerator(TableCsvGenerator):
    """Generates five-minute RMSSD samples across nightly 23:00–08:00 windows."""

    table_name = "hrv"
    fieldnames = (
        "id",
        "user_id",
        "record_date",
        "record_time",
        "rmssd_value",
        "created_at",
        "updated_at",
    )

    def generate(self, output_dir: Path) -> None:
        """Writes samples for seven nights aligned with ``daily_health_summary`` ordering."""
        path = self.output_path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        rng = random.Random()
        today = today_kst_date()

        rows: list[dict[str, str]] = []
        next_id = 1
        for record_date in iter_wake_dates_last_n_days(today, 7):
            start, end = sleep_session_bounds(record_date)
            span_minutes = int((end - start).total_seconds() // 60)
            for offset in range(0, span_minutes + 1, 5):
                ts = start + timedelta(minutes=offset)
                rmssd = rng.uniform(11.5, 91.5)
                rows.append(
                    {
                        "id": str(next_id),
                        "user_id": "1",
                        "record_date": record_date.isoformat(),
                        "record_time": ts.strftime("%Y-%m-%d %H:%M:%S.000000"),
                        "rmssd_value": f"{rmssd:.3f}",
                        "created_at": "",
                        "updated_at": "",
                    },
                )
                next_id += 1

        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(self.fieldnames))
            writer.writeheader()
            writer.writerows(rows)
