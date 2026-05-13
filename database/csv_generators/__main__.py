"""CLI entrypoint: writes one CSV per registered table."""

from __future__ import annotations

import argparse
from pathlib import Path

from csv_generators.dotenv_loader import apply_dotenv, resolve_dotenv_path
from csv_generators.registry import ALL_GENERATORS, TABLE_REGISTRY


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate dummy CSV files (one per table). Loads optional .env for secrets."
        ),
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("."),
        help="Directory where CSV files are written (created if missing).",
    )
    parser.add_argument(
        "--env-file",
        type=Path,
        default=None,
        help="Explicit path to a .env file (searches parents of cwd/output-dir by default).",
    )
    parser.add_argument(
        "--tables",
        nargs="*",
        metavar="NAME",
        help=(
            "Optional subset of table names (e.g. alarm user). "
            "If omitted, all registered tables are generated."
        ),
    )
    return parser.parse_args()


def main() -> None:
    """Runs generators for all or selected tables."""
    args = _parse_args()
    output_dir = args.output_dir.resolve()

    dotenv_path = resolve_dotenv_path(output_dir, args.env_file)
    if dotenv_path is not None:
        apply_dotenv(dotenv_path)

    if args.tables:
        unique_tables = list(dict.fromkeys(args.tables))
        unknown = sorted(set(unique_tables) - set(TABLE_REGISTRY))
        if unknown:
            known = ", ".join(sorted(TABLE_REGISTRY))
            raise ValueError(
                f"Unknown table name(s): {unknown}. Known tables: {known}",
            )
        generators = [TABLE_REGISTRY[name] for name in unique_tables]
    else:
        generators = list(ALL_GENERATORS)

    for generator in generators:
        generator.generate(output_dir)


if __name__ == "__main__":
    main()
