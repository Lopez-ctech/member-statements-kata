"""Data access. SQL lives here and nowhere else.

No business rules in this module: it takes parameters, runs a query, and maps
rows to the types in models.py.
"""

from app import db
from app.models import Account, Transaction

_ACCOUNT_COLUMNS = (
    "id, member_id, account_number, type, currency, status, opening_balance_cents"
)
_TRANSACTION_COLUMNS = "id, account_id, posted_at, type, amount_cents, description"


def find_all_accounts() -> list[Account]:
    rows = db.query(f"SELECT {_ACCOUNT_COLUMNS} FROM accounts ORDER BY id")
    return [Account.from_row(row) for row in rows]


def find_account(account_id: str) -> Account | None:
    rows = db.query(
        f"SELECT {_ACCOUNT_COLUMNS} FROM accounts WHERE id = ?", (account_id,)
    )
    return Account.from_row(rows[0]) if rows else None


def sum_signed_amount_cents(account_id: str) -> int:
    """Signed sum of every transaction on the account.

    Credits count positive, debits negative. There is no date filter here on
    purpose: the current balance is the balance today, not the balance over
    some period.
    """
    rows = db.query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount_cents"
        "                        ELSE -amount_cents END), 0) AS signed_total"
        "  FROM transactions"
        " WHERE account_id = ?",
        (account_id,),
    )
    return int(rows[0]["signed_total"])


def find_transactions_in_range(
    account_id: str, from_date: str, to_date: str
) -> list[Transaction]:
    """Transactions posted in the period, inclusive of both end dates.

    `from_date` and `to_date` are calendar dates (YYYY-MM-DD); posted_at is a
    full ISO-8601 UTC timestamp. Ordered by posted_at then id, so two
    transactions with the same timestamp always come back in the same order.
    """
    rows = db.query(
        f"SELECT {_TRANSACTION_COLUMNS}"
        "  FROM transactions"
        " WHERE account_id = ?"
        "   AND posted_at >= ?"
        "   AND posted_at < ?"
        " ORDER BY posted_at, id",
        (account_id, from_date, to_date),
    )
    return [Transaction.from_row(row) for row in rows]
