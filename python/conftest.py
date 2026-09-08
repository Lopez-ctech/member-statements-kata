"""Shared test fixtures.

Being at the root of python/ this file also puts that directory on sys.path,
so `import app` and `import integration` work no matter how pytest is invoked.
"""

import json
import pathlib
import sys

import pytest
from fastapi.testclient import TestClient

PYTHON_DIR = pathlib.Path(__file__).resolve().parent
REPO_ROOT = PYTHON_DIR.parent
FIXTURES_DIR = REPO_ROOT / "fixtures"

if str(PYTHON_DIR) not in sys.path:
    sys.path.insert(0, str(PYTHON_DIR))


@pytest.fixture(autouse=True)
def fresh_state():
    """Give every test the seed data as it comes out of data/seed.sql."""
    from app import db, services

    db.reset()
    services.reset_statements()
    yield


@pytest.fixture()
def client():
    """An in-process HTTP client. No server, no port, no network."""
    from app.main import app

    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture()
def load_fixture():
    def load(name: str):
        return json.loads((FIXTURES_DIR / name).read_text(encoding="utf-8"))

    return load


@pytest.fixture()
def valid_statement():
    """A minimal statement that satisfies every rule POST /statements checks."""
    return {
        "accountId": "ACC-1001",
        "maskedAccountNumber": "****4821",
        "currency": "USD",
        "period": {"from": "2025-01-01", "to": "2025-01-31"},
        "openingBalance": "1520.00",
        "closingBalance": "1475.00",
        "totals": {
            "creditCount": 0,
            "debitCount": 1,
            "credits": "0.00",
            "debits": "45.00",
        },
        "transactions": [
            {
                "id": 17,
                "date": "2025-01-03",
                "description": "POS DEBIT COFFEE CO",
                "amount": "-45.00",
                "runningBalance": "1475.00",
                "largeTransaction": False,
            }
        ],
        "largeTransactionCount": 0,
    }
