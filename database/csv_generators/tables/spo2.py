"""spo2 table CSV generator."""

from __future__ import annotations

import csv
import random
from datetime import timedelta
from pathlib import Path

from csv_generators.base import TableCsvGenerator
from csv_generators.kst_ranges import iter_wake_dates_last_n_days, sleep_session_bounds, today_kst_date


class Spo2Generator(TableCsvGenerator):
    """Generates one-minute SpO2 samples across nightly 23:00–07:59 windows."""

    table_name = "spo2"
    fieldnames = (
        "id",
        "user_id",
        "record_date",
        "record_time",
        "spo2_value",
        "created_at",
        "updated_at",
    )

    def generate(self, output_dir: Path) -> None:
        """Writes ``540`` samples per night for seven nights (oldest night first)."""
        path = self.output_path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        rng = random.Random()
        today = today_kst_date()

        rows: list[dict[str, str]] = []
        next_id = 1
        for record_date in iter_wake_dates_last_n_days(today, 7):
            start, _end = sleep_session_bounds(record_date)
            for offset in range(540):
                ts = start + timedelta(minutes=offset)
                spo2 = rng.uniform(93.2, 99.8)
                rows.append(
                    {
                        "id": str(next_id),
                        "user_id": "1",
                        "record_date": record_date.isoformat(),
                        "record_time": ts.strftime("%Y-%m-%d %H:%M:%S.000000"),
                        "spo2_value": f"{spo2:.1f}",
                        "created_at": "",
                        "updated_at": "",
                    },
                )
                next_id += 1

        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(self.fieldnames))
            writer.writeheader()
            writer.writerows(rows)
