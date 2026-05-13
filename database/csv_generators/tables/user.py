"""user table CSV generator."""

from __future__ import annotations

import csv
import os
from pathlib import Path

from csv_generators.base import TableCsvGenerator

_ENV_PASSWORD = "user_password"


class UserGenerator(TableCsvGenerator):
    """Generates ``user.csv`` using credentials from the environment."""

    table_name = "user"
    fieldnames = (
        "user_id",
        "password",
        "nickname",
        "created_at",
        "updated_at",
    )

    def generate(self, output_dir: Path) -> None:
        """Writes one user row with ``nickname=sleepy_user``.

        Raises:
            ValueError: If ``user_password`` is missing or blank in the environment.
        """
        password = os.environ.get(_ENV_PASSWORD, "").strip()
        if not password:
            raise ValueError(
                f"Missing or empty {_ENV_PASSWORD}; load a .env file before generating user.csv.",
            )

        path = self.output_path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        row = {
            "user_id": "1",
            "password": password,
            "nickname": "sleepy_user",
            "created_at": "",
            "updated_at": "",
        }
        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(self.fieldnames))
            writer.writeheader()
            writer.writerow(row)
