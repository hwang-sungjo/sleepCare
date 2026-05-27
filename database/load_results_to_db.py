#!/usr/bin/env python3
"""Load CSV files from ``database/results/`` into MySQL using PyMySQL.

Each connection runs a PyMySQL ``init_command`` so session time matches Korea:
``local`` uses numeric ``SET time_zone='+09:00'`` (works without MySQL timezone tables);
``prod`` uses ``SET time_zone='Asia/Seoul'`` like typical JDBC ``serverTimezone``.

Reads connection settings from environment variables (typically via ``.env``).

Pick profile with ``--profile`` (default: ``local``):

- ``local``: ``DATASOURCE_URL_LOCAL``, ``DATASOURCE_USERNAME_LOCAL``,
  ``DATASOURCE_PASSWORD_LOCAL``.
- ``prod``: ``DATASOURCE_URL_PROD``, ``DATASOURCE_USERNAME_PROD``,
  ``DATASOURCE_PASSWORD_PROD``.

Shared:

- ``DATASOURCE_DB_NAME``: Always used as the SQLAlchemy database name (path segment in JDBC URL is ignored).

By default this script loads every CSV under ``database/results/`` **except** ``user.csv``.
Pass ``--include-user`` to load ``user`` as well (or list ``user`` in ``--tables``).

The JDBC URL must be a full MySQL URL starting with ``jdbc:mysql://`` (for example
``jdbc:mysql://host:3306/dbname``). Placeholders like ``${DATASOURCE_DB_NAME}`` in the URL are expanded from ``os.environ`` after ``.env`` is loaded.

Install dependencies::

    pip install -r requirements.txt

Examples::

    python database/load_results_to_db.py
    python database/load_results_to_db.py --profile prod
    python database/load_results_to_db.py --include-user
    python database/load_results_to_db.py --tables alarm
    python database/load_results_to_db.py --tables alarm --include-user
    python database/load_results_to_db.py --tables user alarm
"""

from __future__ import annotations

import argparse
import csv
import os
import re
import sys
from pathlib import Path
from urllib.parse import quote_plus

_ROOT = Path(__file__).resolve().parents[1]  # repository root (parent of database/)
_RESULTS_DIR = _ROOT / "database" / "results"  # fixed: {table}.csv here only

if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from csv_generators.dotenv_loader import apply_dotenv, resolve_dotenv_path  # noqa: E402
from sqlalchemy import create_engine, text  # noqa: E402

_IDENTIFIER_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")

# FK-friendly insert order (adjust if your schema differs).
_INSERT_ORDER: tuple[str, ...] = (
    "user",
    "fitbit",
    "alarm",
    "daily_health_summary",
    "heart_rate",
    "hrv",
    "realtime_metric",
    "sleep_stage",
    "spo2",
)

_ALLOWED_TABLES = frozenset(_INSERT_ORDER)

_CHUNK_SIZE = 400

_AUDIT_TIMESTAMP_COLUMNS = frozenset({"created_at", "updated_at"})


def _validate_identifier(name: str) -> str:
    if not _IDENTIFIER_RE.fullmatch(name):
        raise ValueError(f"Unsafe or invalid SQL identifier: {name!r}")
    return name


def _normalize_cell(value: object) -> str | None:
    if value is None:
        return None
    text_value = str(value).strip()
    return None if text_value == "" else text_value


def _is_blank(value: object) -> bool:
    """True for missing values and whitespace-only strings."""
    if value is None:
        return True
    return str(value).strip() == ""


def _insert_columns_without_all_blank_audits(
    columns: list[str],
    rows: list[dict[str, str | None]],
) -> list[str]:
    """Drops audit timestamp columns when every row is blank so INSERT omits them.

    Omitting the column avoids binding SQL ``NULL`` and lets MySQL ``DEFAULT`` apply.

    Args:
        columns: CSV column order.
        rows: Normalized row dicts.

    Returns:
        Column names to include in ``INSERT``.
    """
    selected: list[str] = []
    for col in columns:
        if col in _AUDIT_TIMESTAMP_COLUMNS and all(_is_blank(row.get(col)) for row in rows):
            continue
        selected.append(col)
    return selected


_JDBC_MYSQL_PREFIX = re.compile(r"(?i)^jdbc:mysql://")
_ENV_REF_IN_VALUE = re.compile(r"\$\{([A-Za-z_][A-Za-z0-9_]*)\}")


def _expand_env_refs(value: str, *, context: str) -> str:
    """Expands ``${VAR_NAME}`` placeholders using ``os.environ``.

    Args:
        value: String that may contain ``${VAR_NAME}`` segments.
        context: Short description for error messages.

    Returns:
        Value with all placeholders replaced.

    Raises:
        ValueError: If a referenced variable is missing or empty.
    """

    def repl(match: re.Match[str]) -> str:
        key = match.group(1)
        sub = os.environ.get(key, "").strip()
        if not sub:
            raise ValueError(
                f"{context}: referenced environment variable {key!r} is missing or empty.",
            )
        return sub

    return _ENV_REF_IN_VALUE.sub(repl, value)


def _parse_mysql_datasource_url(url_raw: str, *, env_url_key: str) -> tuple[str, int, str]:
    """Parses a MySQL JDBC URL into host, port, and optional query string.

    Expects a full JDBC URL beginning with ``jdbc:mysql://`` (case-insensitive).

    The authority segment stops at ``/`` or ``?``; any schema path in the JDBC URL is ignored in favor of
    ``DATASOURCE_DB_NAME``.

    Args:
        url_raw: Raw JDBC URL value from the environment.
        env_url_key: Env key name (for error messages).

    Returns:
        ``(host, port, trailing_query_without_question_mark)``.

    Raises:
        ValueError: If the URL is not JDBC-shaped or host/port cannot be determined.
    """
    cleaned = url_raw.strip().strip('"').strip("'")
    if not _JDBC_MYSQL_PREFIX.match(cleaned):
        raise ValueError(
            f"{env_url_key} must start with jdbc:mysql:// (got {cleaned[:120]!r}).",
        )
    cleaned = _JDBC_MYSQL_PREFIX.sub("", cleaned, count=1)

    query_suffix = ""
    if "?" in cleaned:
        cleaned, query_suffix = cleaned.split("?", 1)
        query_suffix = query_suffix.strip()

    cleaned = cleaned.split("/", 1)[0].strip()
    if not cleaned:
        raise ValueError(f"{env_url_key} does not contain a host after jdbc:mysql://")

    if ":" in cleaned:
        host, port_s = cleaned.rsplit(":", 1)
        host = host.strip()
        try:
            port = int(port_s.strip())
        except ValueError as exc:
            raise ValueError(f"Invalid port in {env_url_key}: {port_s!r}") from exc
    else:
        host = cleaned
        port = 3306

    if port < 1 or port > 65535:
        raise ValueError(f"Port out of range in {env_url_key}: {port}")
    if not host:
        raise ValueError(f"{env_url_key} host is empty.")

    return host, port, query_suffix


def _require_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise ValueError(f"Missing or empty environment variable: {name}")
    return value


def _profile_env_keys(profile: str) -> tuple[str, str, str]:
    """Returns ``(url_key, username_key, password_key)`` for the given profile."""
    if profile == "local":
        return (
            "DATASOURCE_URL_LOCAL",
            "DATASOURCE_USERNAME_LOCAL",
            "DATASOURCE_PASSWORD_LOCAL",
        )
    if profile == "prod":
        return (
            "DATASOURCE_URL_PROD",
            "DATASOURCE_USERNAME_PROD",
            "DATASOURCE_PASSWORD_PROD",
        )
    raise ValueError(f"Unsupported profile: {profile!r} (expected 'local' or 'prod').")


def _pymysql_timezone_init_command(profile: str) -> str:
    """SQL run on each new PyMySQL connection to set the session time zone to Korea.

    Args:
        profile: ``local`` or ``prod``.

    Returns:
        A single ``SET time_zone=...`` statement.

    Raises:
        ValueError: If ``profile`` is not supported.
    """
    if profile == "local":
        return "SET time_zone='+09:00'"
    if profile == "prod":
        return "SET time_zone='Asia/Seoul'"
    raise ValueError(f"Unsupported profile: {profile!r} (expected 'local' or 'prod').")


def _build_database_url(profile: str) -> str:
    url_key, user_key, pass_key = _profile_env_keys(profile)
    url_raw = _expand_env_refs(_require_env(url_key), context=url_key)
    host, port, jdbc_query = _parse_mysql_datasource_url(url_raw, env_url_key=url_key)
    database = _require_env("DATASOURCE_DB_NAME")
    username = _require_env(user_key)
    password = _require_env(pass_key)

    user_q = quote_plus(username)
    pass_q = quote_plus(password)
    db_q = quote_plus(database)
    base = f"mysql+pymysql://{user_q}:{pass_q}@{host}:{port}/{db_q}"

    query_parts = ["charset=utf8mb4"]
    if jdbc_query:
        query_parts.append(jdbc_query)

    return f"{base}?{'&'.join(query_parts)}"


def _ordered_selected_tables(requested: frozenset[str]) -> list[str]:
    unknown = sorted(requested - _ALLOWED_TABLES)
    if unknown:
        allowed = ", ".join(sorted(_ALLOWED_TABLES))
        raise ValueError(f"Unknown table(s): {unknown}. Allowed: {allowed}")
    return [name for name in _INSERT_ORDER if name in requested]


def _load_csv_rows(csv_path: Path) -> tuple[list[str], list[dict[str, str | None]]]:
    with csv_path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames is None:
            raise ValueError(f"No header row in {csv_path}")

        pairs: list[tuple[str, str]] = []
        for raw_key in reader.fieldnames:
            stripped_key = raw_key.strip()
            col = _validate_identifier(stripped_key)
            pairs.append((raw_key, col))

        rows_out: list[dict[str, str | None]] = []
        for raw_row in reader:
            row: dict[str, str | None] = {}
            for raw_key, col in pairs:
                row[col] = _normalize_cell(raw_row.get(raw_key))
            rows_out.append(row)

        columns = [col for _raw, col in pairs]
        return columns, rows_out


def _insert_chunked(conn, table: str, columns: list[str], rows: list[dict[str, str | None]]) -> int:
    table_sql = _validate_identifier(table)
    if not rows:
        raise ValueError(f"No data rows in CSV for `{table_sql}`.")
    if not columns:
        raise ValueError(f"No insert columns left for `{table_sql}` after trimming audit fields.")

    col_sql = ", ".join(f"`{c}`" for c in columns)
    placeholders = ", ".join(f":{c}" for c in columns)
    stmt = text(f"INSERT INTO `{table_sql}` ({col_sql}) VALUES ({placeholders})")

    inserted = 0
    for start in range(0, len(rows), _CHUNK_SIZE):
        slice_rows = rows[start : start + _CHUNK_SIZE]
        chunk = [{c: row[c] for c in columns} for row in slice_rows]
        conn.execute(stmt, chunk)
        inserted += len(chunk)
    return inserted


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Insert CSVs from database/results/ using DATASOURCE_* environment variables.",
    )
    parser.add_argument(
        "--tables",
        nargs="*",
        metavar="TABLE",
        help=(
            "Tables to load (CSV stem names). "
            "Omit to load every known table except ``user`` (see ``--include-user``)."
        ),
    )
    parser.add_argument(
        "--include-user",
        action="store_true",
        help="Also load ``user.csv`` (unioned with ``--tables`` when both are used).",
    )
    parser.add_argument(
        "--env-file",
        type=Path,
        default=None,
        help="Optional explicit ``.env`` path (otherwise discovered like csv_generators CLI).",
    )
    parser.add_argument(
        "--profile",
        choices=("local", "prod"),
        default="local",
        help="Which DATASOURCE_* env suffix to use (default: local).",
    )
    return parser.parse_args()


def main() -> None:
    args = _parse_args()
    results_dir = _RESULTS_DIR.resolve()
    if not results_dir.is_dir():
        raise FileNotFoundError(f"Results directory not found: {results_dir}")

    dotenv_path = resolve_dotenv_path(results_dir, args.env_file)
    if dotenv_path is None:
        raise FileNotFoundError(
            "Could not locate .env (try ``--env-file`` or place .env in the repo root / near database/).",
        )
    apply_dotenv(dotenv_path)

    if args.tables:
        selected = frozenset(name.strip() for name in args.tables if name.strip())
        if not selected:
            raise ValueError("``--tables`` was passed but no valid names were given.")
    else:
        # ``user`` is skipped by default (often already populated); use ``--include-user`` to load it.
        selected = frozenset(_ALLOWED_TABLES) - {"user"}

    if args.include_user:
        selected = selected | {"user"}

    tables = _ordered_selected_tables(selected)

    engine = create_engine(
        _build_database_url(args.profile),
        pool_pre_ping=True,
        connect_args={"init_command": _pymysql_timezone_init_command(args.profile)},
    )

    total_rows = 0
    with engine.begin() as conn:
        for table in tables:
            csv_path = results_dir / f"{table}.csv"
            if not csv_path.is_file():
                raise FileNotFoundError(f"Missing CSV for `{table}`: {csv_path}")

            columns, rows = _load_csv_rows(csv_path)
            insert_columns = _insert_columns_without_all_blank_audits(columns, rows)
            inserted = _insert_chunked(conn, table, insert_columns, rows)
            total_rows += inserted
            print(f"Inserted {inserted} row(s) into `{table}`.")

    print(f"Done. Total rows inserted: {total_rows}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # noqa: BLE001 — CLI surfaces a single friendly failure.
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
