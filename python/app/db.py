"""The database: in-memory SQLite, seeded from data/seed.sql on first use.

Nothing is installed and nothing is persisted. Every start of the API gets a
fresh copy of the same data, which is what makes the tests reproducible.
"""

import os
import pathlib
import sqlite3
import threading

SEED_RELATIVE_PATH = pathlib.Path("data") / "seed.sql"

_LOCK = threading.Lock()
_CONNECTION: sqlite3.Connection | None = None


def locate_seed() -> pathlib.Path:
    """Find data/seed.sql without hardcoding a path separator or a depth.

    Walks up from this file's own location, so it does not matter which
    directory the API was started from. SEED_SQL overrides it if you need to.
    """
    override = os.environ.get("SEED_SQL")
    if override:
        return pathlib.Path(override).resolve()
    for directory in pathlib.Path(__file__).resolve().parents:
        candidate = directory / SEED_RELATIVE_PATH
        if candidate.is_file():
            return candidate
    raise FileNotFoundError(
        f"Could not find {SEED_RELATIVE_PATH} above {__file__}. "
        "Set the SEED_SQL environment variable to point at it."
    )


def _connection() -> sqlite3.Connection:
    global _CONNECTION
    if _CONNECTION is None:
        connection = sqlite3.connect(":memory:", check_same_thread=False)
        connection.row_factory = sqlite3.Row
        connection.executescript(locate_seed().read_text(encoding="utf-8"))
        _CONNECTION = connection
    return _CONNECTION


def query(sql: str, params: tuple = ()) -> list[sqlite3.Row]:
    """Run a read query. The lock keeps the single shared connection safe."""
    with _LOCK:
        return _connection().execute(sql, params).fetchall()


def reset() -> None:
    """Throw the database away so the next query re-seeds it. Used by tests."""
    global _CONNECTION
    with _LOCK:
        if _CONNECTION is not None:
            _CONNECTION.close()
            _CONNECTION = None
