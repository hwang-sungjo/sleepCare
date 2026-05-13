"""Shared Korea Standard Time helpers for aligned dummy CSV rows."""

from __future__ import annotations

from datetime import date, datetime, time, timedelta
from zoneinfo import ZoneInfo

KST = ZoneInfo("Asia/Seoul")
BEDTIME = time(23, 0, 0)
WAKE_TIME = time(8, 0, 0)


def now_kst() -> datetime:
    """Returns the current instant in ``Asia/Seoul``."""
    return datetime.now(KST)


def today_kst_date() -> date:
    """Returns today's calendar date in ``Asia/Seoul``."""
    return now_kst().date()


def latest_closed_minute_timestamp(now: datetime) -> datetime:
    """Returns one minute before ``now`` truncated to the minute boundary.

    Example:
        ``20:00:45`` → ``19:59:00`` (same timezone as ``now``).

    Args:
        now: Reference instant.

    Returns:
        ``now`` with ``second`` and ``microsecond`` cleared, minus one minute.
    """
    floored = now.replace(second=0, microsecond=0)
    return floored - timedelta(minutes=1)


def sleep_session_bounds(record_date: date) -> tuple[datetime, datetime]:
    """Returns bedtime-to-wake datetimes for one sleep session.

    Bedtime is ``record_date - 1 day`` at 23:00 KST; wake is ``record_date`` at 08:00 KST.

    Args:
        record_date: Wake-up calendar date attributed to the sleep session.

    Returns:
        Tuple ``(bedtime_start, wake_end)`` both timezone-aware in KST.
    """
    prev_evening = record_date - timedelta(days=1)
    start = datetime.combine(prev_evening, BEDTIME, tzinfo=KST)
    end = datetime.combine(record_date, WAKE_TIME, tzinfo=KST)
    return start, end


def sleep_span_seconds(record_date: date) -> int:
    """Returns the sleep window length in whole seconds."""
    start, end = sleep_session_bounds(record_date)
    return int((end - start).total_seconds())


def iter_wake_dates_last_n_days(today: date, days: int = 7):
    """Yields wake dates from oldest to newest (CSV top → bottom).

    For ``days=7`` this yields ``today - 6`` through ``today`` (seven nights ending
    on the anchor day and the six prior wake dates).

    Args:
        today: Anchor calendar date (typically ``today_kst_date()``).
        days: Number of consecutive wake dates to emit, ending on ``today``.

    Yields:
        Wake dates ordered oldest-first.
    """
    if days < 1:
        raise ValueError("days must be at least 1.")
    for days_ago in range(days - 1, -1, -1):
        yield today - timedelta(days=days_ago)


def iter_calendar_days_last_n_days(today: date, days: int = 7):
    """Yields each calendar day from ``today - (days - 1)`` through ``today``, oldest-first."""
    yield from iter_wake_dates_last_n_days(today, days)
