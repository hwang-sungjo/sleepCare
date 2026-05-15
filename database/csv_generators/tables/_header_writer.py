"""Shared CSV header writing for skeleton generators."""

from __future__ import annotations

import csv
from pathlib import Path

from csv_generators.base import TableCsvGenerator


def write_header_only(generator: TableCsvGenerator, output_dir: Path) -> None:
    """Writes UTF-8 CSV with header row only (no data rows).

    Args:
        generator: Generator providing ``table_name`` and ``fieldnames``.
        output_dir: Directory where ``{table_name}.csv`` is written.

    Raises:
        OSError: If creating the directory or file fails.
    """
    path = generator.output_path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(generator.fieldnames))
        writer.writeheader()
