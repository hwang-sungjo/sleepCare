"""sleep_stage table CSV generator."""

from __future__ import annotations

import csv
import random
from datetime import timedelta
from pathlib import Path

from csv_generators.base import TableCsvGenerator
from csv_generators.kst_ranges import (
    iter_wake_dates_last_n_days,
    sleep_session_bounds,
    sleep_span_seconds,
    today_kst_date,
)

_STAGE_CHOICES = ("wake", "light", "deep", "rem")
_STAGE_WEIGHTS = (0.09, 0.44, 0.29, 0.18)


def _random_segments(total_seconds: int, rng: random.Random) -> list[tuple[int, str]]:
    """Splits a sleep session into contiguous random stage segments.

    Args:
        total_seconds: Session length in whole seconds (typically nine hours).
        rng: Random source.

    Returns:
        Ordered ``(duration_seconds, stage_level)`` tuples covering the session.

    Raises:
        ValueError: If ``total_seconds`` is too small to chunk safely.
    """
    if total_seconds < 60:
        raise ValueError("Sleep session too short for randomized staging.")

    segments: list[tuple[int, str]] = []
    remaining = total_seconds
    min_seg = 30

    while remaining > 0:
        max_seg = min(40 * 60, remaining)
        if remaining <= min_seg:
            dur = remaining
        else:
            upper = max(min_seg, max_seg)
            dur = rng.randint(min_seg, upper)
            dur = min(dur, remaining)

        stage = rng.choices(_STAGE_CHOICES, weights=_STAGE_WEIGHTS, k=1)[0]
        segments.append((dur, stage))
        remaining -= dur

    return segments


class SleepStageGenerator(TableCsvGenerator):
    """Generates synthetic hypnogram segments for seven nightly sleep sessions."""

    table_name = "sleep_stage"
    fieldnames = (
        "id",
        "user_id",
        "record_date",
        "start_time",
        "duration_seconds",
        "stage_level",
        "created_at",
        "updated_at",
    )

    def generate(self, output_dir: Path) -> None:
        """Writes contiguous stages from 23:00 through 08:00 for seven nights."""
        path = self.output_path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        rng = random.Random()
        today = today_kst_date()

        rows: list[dict[str, str]] = []
        next_id = 1
        for record_date in iter_wake_dates_last_n_days(today, 7):
            start, end = sleep_session_bounds(record_date)
            total_seconds = sleep_span_seconds(record_date)
            segments = _random_segments(total_seconds, rng)

            cursor = start
            for dur_sec, stage in segments:
                rows.append(
                    {
                        "id": str(next_id),
                        "user_id": "1",
                        "record_date": record_date.isoformat(),
                        "start_time": cursor.strftime("%Y-%m-%d %H:%M:%S.000000"),
                        "duration_seconds": str(dur_sec),
                        "stage_level": stage,
                        "created_at": "",
                        "updated_at": "",
                    },
                )
                cursor += timedelta(seconds=dur_sec)
                next_id += 1

            if cursor != end:
                raise RuntimeError(
                    "Sleep stage segmentation drifted from the nightly window bounds.",
                )

        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(self.fieldnames))
            writer.writeheader()
            writer.writerows(rows)
