"""Minimal ``.env`` file loader (no third-party dependency)."""

from __future__ import annotations

import os
from pathlib import Path


def resolve_dotenv_path(output_dir: Path, explicit: Path | None = None) -> Path | None:
    """Locates a ``.env`` file next to ``output_dir``, ``cwd``, or their parents.

    Args:
        output_dir: CSV output directory used as one search anchor.
        explicit: If set, must exist as a file path.

    Returns:
        Resolved path to ``.env``, or ``None`` if not found (when ``explicit`` is None).

    Raises:
        FileNotFoundError: If ``explicit`` is set but does not exist.
    """
    if explicit is not None:
        resolved = explicit.expanduser().resolve()
        if not resolved.is_file():
            raise FileNotFoundError(f".env not found at {resolved}")
        return resolved

    seen: set[Path] = set()
    for anchor in (output_dir.resolve(), Path.cwd().resolve()):
        if anchor in seen:
            continue
        seen.add(anchor)
        cur = anchor
        for _ in range(16):
            candidate = (cur / ".env").resolve()
            if candidate.is_file():
                return candidate
            parent = cur.parent
            if parent == cur:
                break
            cur = parent
    return None


def apply_dotenv(path: Path, *, override: bool = False) -> None:
    """Loads KEY=value pairs from ``path`` into ``os.environ``.

    Blank lines and ``#`` comments are ignored. Inline whitespace around keys and
    values is stripped; optional surrounding ``'`` or ``"`` on values is removed.

    Args:
        path: UTF-8 encoded ``.env`` file.
        override: When ``True``, existing environment variables are overwritten.

    Raises:
        OSError: If the file cannot be read.
    """
    raw = path.read_text(encoding="utf-8")
    for raw_line in raw.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        if not key:
            continue
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
            value = value[1:-1]
        if override or key not in os.environ:
            os.environ[key] = value
