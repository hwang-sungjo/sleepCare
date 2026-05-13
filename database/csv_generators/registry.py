"""Registration of all table CSV generators."""

from __future__ import annotations

from csv_generators.base import TableCsvGenerator
from csv_generators.tables.alarm import AlarmGenerator
from csv_generators.tables.daily_health_summary import DailyHealthSummaryGenerator
from csv_generators.tables.fitbit import FitbitGenerator
from csv_generators.tables.heart_rate import HeartRateGenerator
from csv_generators.tables.hrv import HrvGenerator
from csv_generators.tables.realtime_metric import RealtimeMetricGenerator
from csv_generators.tables.sleep_stage import SleepStageGenerator
from csv_generators.tables.spo2 import Spo2Generator
from csv_generators.tables.user import UserGenerator

ALL_GENERATORS: tuple[TableCsvGenerator, ...] = (
    AlarmGenerator(),
    DailyHealthSummaryGenerator(),
    FitbitGenerator(),
    HeartRateGenerator(),
    HrvGenerator(),
    RealtimeMetricGenerator(),
    SleepStageGenerator(),
    Spo2Generator(),
    UserGenerator(),
)

TABLE_REGISTRY: dict[str, TableCsvGenerator] = {g.table_name: g for g in ALL_GENERATORS}
