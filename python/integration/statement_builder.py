"""Part C: build a monthly member statement and deliver it.

The job has three steps:

    1. fetch      call the API for the account and its transactions   (written for you)
    2. transform  turn those two responses into a statement document  (YOU write this)
    3. deliver    POST the document to /statements                    (written for you)

Step 2 is a pure function: same inputs, same output, no network and no files.
That is what lets tests/test_statement_builder.py run with the API switched off.

Run the whole job (the API must be running):

    python -m integration.statement_builder ACC-1001 2025-01-01 2025-01-31

The rules for the statement document are in the README, section "Part C", and
the shape is in docs/statement-schema.json.
"""

import argparse
import json
import os
import sys

import httpx

DEFAULT_BASE_URL = os.environ.get("API_BASE_URL") or (
    "http://localhost:" + os.environ.get("PORT", "8080")
)


# --------------------------------------------------------------------------
# Step 2: transform. This is the part you write.
# --------------------------------------------------------------------------


def build_statement(account: dict, transactions: list[dict], period: dict) -> dict:
    """Turn one account and its transactions into a statement document.

    Args:
        account: the body of GET /accounts/{id}, e.g.
            {"id": "ACC-1001", "accountNumber": "483920174821", "currency": "USD",
             "openingBalanceCents": 152000, "currentBalanceCents": 1395700, ...}
        transactions: the body of GET /accounts/{id}/transactions, a list of
            {"id": 17, "postedAt": "2025-01-03T09:12:00Z", "type": "DEBIT",
             "amountCents": 4500, "description": "POS DEBIT COFFEE CO", ...}
        period: {"from": "2025-01-01", "to": "2025-01-31"}

    Returns:
        The statement document described in the README and in
        docs/statement-schema.json.

    Keep this function pure: no HTTP, no file access, no clock, no globals.
    """
    raise NotImplementedError(
        "Part C: implement build_statement. See the README, section 'Part C'."
    )


# --------------------------------------------------------------------------
# Steps 1 and 3: fetch and deliver. Already wired up.
# --------------------------------------------------------------------------


def fetch_inputs(base_url: str, account_id: str, period: dict) -> tuple[dict, list[dict]]:
    """Fetch the account and its transactions for the period."""
    account = httpx.get(f"{base_url}/accounts/{account_id}", timeout=10.0).json()
    transactions = httpx.get(
        f"{base_url}/accounts/{account_id}/transactions",
        params={"from": period["from"], "to": period["to"]},
        timeout=10.0,
    ).json()
    return account, transactions


def deliver(base_url: str, statement: dict) -> str:
    """POST the statement and return the id the service assigned to it."""
    response = httpx.post(f"{base_url}/statements", json=statement, timeout=10.0)
    if response.status_code != 201:
        raise RuntimeError(
            f"POST /statements returned {response.status_code}: {response.text}"
        )
    return response.json()["statementId"]


def run_statement(
    account_id: str, period_from: str, period_to: str, base_url: str = DEFAULT_BASE_URL
) -> tuple[dict, str]:
    """Fetch, transform, deliver. Returns the statement and its statementId."""
    period = {"from": period_from, "to": period_to}
    account, transactions = fetch_inputs(base_url, account_id, period)
    statement = build_statement(account, transactions, period)
    statement_id = deliver(base_url, statement)
    return statement, statement_id


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("account_id", help="e.g. ACC-1001")
    parser.add_argument("period_from", help="YYYY-MM-DD, inclusive")
    parser.add_argument("period_to", help="YYYY-MM-DD, inclusive")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--out", help="also write the statement to this file")
    parser.add_argument(
        "--expect",
        help="compare the statement with this JSON file; exit non-zero if they differ",
    )
    args = parser.parse_args(argv)

    statement, statement_id = run_statement(
        args.account_id, args.period_from, args.period_to, args.base_url
    )
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            json.dump(statement, handle, indent=2)
            handle.write("\n")
    print(f"statementId: {statement_id}")

    if args.expect:
        with open(args.expect, encoding="utf-8") as handle:
            expected = json.load(handle)
        if statement != expected:
            print(f"MISMATCH: the statement does not match {args.expect}", file=sys.stderr)
            _report_differences(expected, statement)
            return 1
        print(f"match: the statement equals {args.expect}")
    return 0


def _report_differences(expected: dict, actual: dict) -> None:
    for key in sorted(set(expected) | set(actual)):
        if expected.get(key) != actual.get(key):
            print(f"  {key}:", file=sys.stderr)
            print(f"    expected {_short(expected.get(key))}", file=sys.stderr)
            print(f"    got      {_short(actual.get(key))}", file=sys.stderr)


def _short(value, limit: int = 300) -> str:
    text = json.dumps(value, sort_keys=True)
    return text if len(text) <= limit else text[:limit] + " ..."


if __name__ == "__main__":
    sys.exit(main())
