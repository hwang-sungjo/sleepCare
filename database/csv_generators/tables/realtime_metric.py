"""realtime_metric table CSV generator."""

from __future__ import annotations

import csv
import random
from datetime import datetime, time, timedelta
from pathlib import Path

from csv_generators.base import TableCsvGenerator
from csv_generators.kst_ranges import KST, iter_calendar_days_last_n_days, now_kst, today_kst_date


def _format_audit_timestamp(dt: datetime) -> str:
    """Formats audit columns with microsecond precision."""
    return dt.strftime("%Y-%m-%d %H:%M:%S.%f")


class RealtimeMetricGenerator(TableCsvGenerator):
    """Generates minute-resolution environment metrics for the last seven days."""

    table_name = "realtime_metric"
    fieldnames = (
        "realtime_metric_id",
        "user_id",
        "illuminance",
        "temperature",
        "humidity",
        "created_at",
        "updated_at",
    )

    def generate(self, output_dir: Path) -> None:
        """Writes ``7 * 1440`` rows ordered oldest-first (CSV bottom is newest).

        ``created_at`` / ``updated_at`` are synthetic audit stamps: ``now`` floored to
        the minute minus ``N`` minutes where ``N`` counts down from ``10080`` on the
        first row to ``1`` on the last row (newest sample → one minute before anchor).
        """
        path = self.output_path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        rng = random.Random()
        today = today_kst_date()

        row_count = 7 * 1440
        audit_anchor = now_kst().replace(second=0, microsecond=0)

        rows: list[dict[str, str]] = []
        next_id = 1
        row_index = 0
        for cal_day in iter_calendar_days_last_n_days(today, 7):
            midnight = datetime.combine(cal_day, time.min, tzinfo=KST)
            for minute_idx in range(1440):
                illuminance = rng.uniform(8.0, 520.0)
                temperature = rng.uniform(18.5, 31.5)
                humidity = rng.randint(15, 75)
                minutes_ago = row_count - row_index
                audit_ts = audit_anchor - timedelta(minutes=minutes_ago)
                audit_str = _format_audit_timestamp(audit_ts)
                rows.append(
                    {
                        "realtime_metric_id": str(next_id),
                        "user_id": "1",
                        "illuminance": f"{illuminance:.2f}",
                        "temperature": f"{temperature:.1f}",
                        "humidity": str(humidity),
                        "created_at": audit_str,
                        "updated_at": audit_str,
                    },
                )
                next_id += 1
                row_index += 1

        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(self.fieldnames))
            writer.writeheader()
            writer.writerows(rows)
