"""heart_rate table CSV generator."""

from __future__ import annotations

import csv
import random
from datetime import timedelta
from pathlib import Path

from csv_generators.base import TableCsvGenerator
from csv_generators.kst_ranges import latest_closed_minute_timestamp, now_kst


_SAMPLE_COUNT = 7 * 24 * 60


class HeartRateGenerator(TableCsvGenerator):
    """Generates minute-resolution heart-rate samples for the last seven days."""

    table_name = "heart_rate"
    fieldnames = (
        "id",
        "user_id",
        "record_date",
        "record_time",
        "bpm",
        "created_at",
        "updated_at",
    )

    def generate(self, output_dir: Path) -> None:
        """Writes ``7 * 1440`` rows ending at the last closed local minute (KST)."""
        path = self.output_path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        rng = random.Random()

        end_ts = latest_closed_minute_timestamp(now_kst())
        oldest_ts = end_ts - timedelta(minutes=_SAMPLE_COUNT - 1)

        rows: list[dict[str, str]] = []
        for idx in range(_SAMPLE_COUNT):
            ts = oldest_ts + timedelta(minutes=idx)
            bpm = rng.randint(52, 118)
            rows.append(
                {
                    "id": str(idx + 1),
                    "user_id": "1",
                    "record_date": ts.date().isoformat(),
                    "record_time": ts.strftime("%Y-%m-%d %H:%M:%S.000000"),
                    "bpm": str(bpm),
                    "created_at": "",
                    "updated_at": "",
                },
            )

        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(self.fieldnames))
            writer.writeheader()
            writer.writerows(rows)
