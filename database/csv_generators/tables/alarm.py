"""alarm table CSV generator."""

from __future__ import annotations

import csv
from datetime import date, datetime, time, timedelta
from pathlib import Path

from csv_generators.base import TableCsvGenerator
from csv_generators.kst_ranges import KST as _KST

_WAKE_TIME = time(7, 30, 0)
_MAX_LOOKBACK_DAYS = 366


def _combine_kst(day: date, clock: time) -> datetime:
    """Builds a timezone-aware datetime in Asia/Seoul.

    Args:
        day: Calendar date in the local (KST) calendar.
        clock: Local time-of-day.

    Returns:
        Aware datetime with ``tzinfo=Asia/Seoul``.
    """
    return datetime.combine(day, clock, tzinfo=_KST)


def _dynamic_wake_at(day_of_week: int, now_kst: datetime) -> datetime:
    """Computes ``dynamic_wake_at`` for one alarm row.

    Exactly one row (tomorrow's weekday) is scheduled at tomorrow 07:30 KST.
    Every other weekday uses the latest occurrence of that weekday at 07:30 KST
    that is strictly before ``now_kst``.

    Args:
        day_of_week: ISO weekday (Monday=1 .. Sunday=7), matching the CSV column.
        now_kst: Current instant in Korea Standard Time.

    Returns:
        Local wake datetime for that row.

    Raises:
        RuntimeError: If no past occurrence is found within the lookback window.
    """
    today = now_kst.date()
    tomorrow = today + timedelta(days=1)
    tomorrow_weekday = tomorrow.isoweekday()

    if day_of_week == tomorrow_weekday:
        return _combine_kst(tomorrow, _WAKE_TIME)

    for delta in range(_MAX_LOOKBACK_DAYS):
        candidate_day = today - timedelta(days=delta)
        if candidate_day.isoweekday() != day_of_week:
            continue
        candidate_dt = _combine_kst(candidate_day, _WAKE_TIME)
        if candidate_dt < now_kst:
            return candidate_dt

    raise RuntimeError(
        "Could not find a past wake time for "
        f"day_of_week={day_of_week} within {_MAX_LOOKBACK_DAYS} days.",
    )


def _format_dynamic_wake_at(dt: datetime) -> str:
    """Formats wake datetime as KST wall clock with fractional seconds."""
    return dt.strftime("%Y-%m-%d %H:%M:%S.000000")


class AlarmGenerator(TableCsvGenerator):
    """Generates ``alarm.csv`` with seven fixed weekly alarms."""

    table_name = "alarm"
    fieldnames = (
        "alarm_id",
        "user_id",
        "day_of_week",
        "base_wake_time",
        "dynamic_wake_at",
        "adaptive_enabled",
        "window_minutes_before",
        "created_at",
        "updated_at",
    )

    def generate(self, output_dir: Path) -> None:
        """Writes ``alarm.csv`` with rows ordered by ``alarm_id`` ascending."""
        path = self.output_path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        now_kst = datetime.now(_KST)

        rows: list[dict[str, str]] = []
        for alarm_id in range(1, 8):
            dow = alarm_id
            dynamic_dt = _dynamic_wake_at(dow, now_kst)
            rows.append(
                {
                    "alarm_id": str(alarm_id),
                    "user_id": "1",
                    "day_of_week": str(dow),
                    "base_wake_time": "07:30:00",
                    "dynamic_wake_at": _format_dynamic_wake_at(dynamic_dt),
                    "adaptive_enabled": "1",
                    "window_minutes_before": "30",
                    "created_at": "",
                    "updated_at": "",
                },
            )

        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(self.fieldnames))
            writer.writeheader()
            writer.writerows(rows)
