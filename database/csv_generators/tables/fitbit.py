"""fitbit table CSV generator."""

from __future__ import annotations

import csv
import os
from datetime import timedelta
from pathlib import Path

from csv_generators.base import TableCsvGenerator
from csv_generators.kst_ranges import now_kst

_ENV_ACCESS = "fitbit_access_token"
_ENV_REFRESH = "fitbit_refresh_token"


class FitbitGenerator(TableCsvGenerator):
    """Generates ``fitbit.csv`` using Fitbit tokens loaded from the environment."""

    table_name = "fitbit"
    fieldnames = (
        "user_id",
        "fitbit_access_token",
        "fitbit_refresh_token",
        "fitbit_token_expires_at",
        "created_at",
        "updated_at",
    )

    def generate(self, output_dir: Path) -> None:
        """Writes one Fitbit linkage row for ``user_id=1``.

        Raises:
            ValueError: If required env vars are missing or blank.
        """
        access = os.environ.get(_ENV_ACCESS, "").strip()
        refresh = os.environ.get(_ENV_REFRESH, "").strip()
        if not access:
            raise ValueError(
                f"Missing or empty {_ENV_ACCESS}; load a .env file before generating fitbit.csv.",
            )
        if not refresh:
            raise ValueError(
                f"Missing or empty {_ENV_REFRESH}; load a .env file before generating fitbit.csv.",
            )

        expires_at = now_kst() + timedelta(days=1)

        path = self.output_path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        row = {
            "user_id": "1",
            "fitbit_access_token": access,
            "fitbit_refresh_token": refresh,
            "fitbit_token_expires_at": expires_at.strftime("%Y-%m-%d %H:%M:%S.000000"),
            "created_at": "",
            "updated_at": "",
        }
        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(self.fieldnames))
            writer.writeheader()
            writer.writerow(row)
