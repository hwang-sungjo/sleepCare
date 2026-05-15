"""Abstract base for per-table CSV dummy generators."""

from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path


class TableCsvGenerator(ABC):
    """Writes exactly one CSV file named ``{table_name}.csv`` under a directory.

    Subclasses define ``table_name``, ``fieldnames``, and implement ``generate``.
    """

    table_name: str
    fieldnames: tuple[str, ...]

    def output_path(self, output_dir: Path) -> Path:
        """Returns the target path ``output_dir / f'{table_name}.csv'``.

        Args:
            output_dir: Directory that will contain the CSV file.

        Returns:
            Absolute or relative path joining ``output_dir`` and the CSV filename.
        """
        return output_dir / f"{self.table_name}.csv"

    @abstractmethod
    def generate(self, output_dir: Path) -> None:
        """Create or overwrite this table's CSV under ``output_dir``.

        Args:
            output_dir: Destination directory for ``{table_name}.csv``.

        Raises:
            OSError: If the file cannot be written.
        """
