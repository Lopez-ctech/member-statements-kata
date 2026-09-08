"""Business rules.

Everything the API knows that is not "how do I speak HTTP" and not "how do I
speak SQL" lives here: balance arithmetic, date-range validation, the
does-this-account-exist check, and the statement payload contract.

The exceptions raised here are deliberately about the problem, not about HTTP.
main.py is the only place that decides which status code each one becomes.
"""

import itertools
import re
from datetime import date

from app import repositories
from app.models import Account, Transaction

#: Money in a statement is a string with exactly two decimals, e.g. "-45.00".
AMOUNT_PATTERN = re.compile(r"^-?\d+\.\d{2}$")

#: Four asterisks and the last four digits, e.g. "****4821".
MASKED_ACCOUNT_NUMBER_PATTERN = re.compile(r"^\*{4}\d{4}$")


class BadRequestError(Exception):
    """The caller sent something we cannot act on. Becomes a 400."""


class NotFoundError(Exception):
    """The caller asked for something that does not exist. Becomes a 404."""


class ValidationError(Exception):
    """A payload was well-formed JSON but broke the contract. Becomes a 422."""

    def __init__(self, field_errors: list[dict]):
        super().__init__("Statement failed validation")
        self.field_errors = field_errors


# --------------------------------------------------------------------------
# Accounts
# --------------------------------------------------------------------------


def list_accounts() -> list[Account]:
    return repositories.find_all_accounts()


def get_account(account_id: str) -> Account:
    account = repositories.find_account(account_id)
    if account is None:
        raise NotFoundError(f"Unknown account '{account_id}'")
    return account


def current_balance_cents(account: Account) -> int:
    """Opening balance plus every credit, minus every debit.

    Computed on every read rather than stored, so it can never drift out of
    step with the transactions it is derived from.
    """
    return account.opening_balance_cents + repositories.sum_signed_amount_cents(
        account.id
    )


def get_transactions(
    account_id: str, from_param: str | None, to_param: str | None
) -> list[Transaction]:
    """Transactions for an account over an inclusive date range.

    Parameters are validated before the account is looked up, so a request
    with no dates gets a 400 whether or not the account exists.
    """
    from_date = _parse_date("from", from_param)
    to_date = _parse_date("to", to_param)
    if from_date > to_date:
        raise BadRequestError(
            f"'from' ({from_date}) must not be after 'to' ({to_date})"
        )
    get_account(account_id)
    return repositories.find_transactions_in_range(
        account_id, from_date.isoformat(), to_date.isoformat()
    )


def _parse_date(name: str, value: str | None) -> date:
    if not value:
        raise BadRequestError(f"Query parameter '{name}' is required (YYYY-MM-DD)")
    try:
        return date.fromisoformat(value)
    except ValueError:
        raise BadRequestError(
            f"Query parameter '{name}' must be a date in YYYY-MM-DD format, got '{value}'"
        ) from None


# --------------------------------------------------------------------------
# Statements
#
# A stub receiver. A real one would hand the document to a delivery system;
# this one checks the contract and keeps it in memory so the integration
# exercise has something to POST to.
# --------------------------------------------------------------------------

_STATEMENTS: dict[str, dict] = {}
_STATEMENT_NUMBERS = itertools.count(1)


def record_statement(payload) -> str:
    errors = validate_statement(payload)
    if errors:
        raise ValidationError(errors)
    statement_id = "STMT-%04d" % next(_STATEMENT_NUMBERS)
    _STATEMENTS[statement_id] = payload
    return statement_id


def find_statement(statement_id: str) -> dict | None:
    return _STATEMENTS.get(statement_id)


def reset_statements() -> None:
    """Empty the in-memory store. Used by tests."""
    global _STATEMENT_NUMBERS
    _STATEMENTS.clear()
    _STATEMENT_NUMBERS = itertools.count(1)


def validate_statement(payload) -> list[dict]:
    """Return one entry per broken rule. An empty list means the payload is good."""
    if not isinstance(payload, dict):
        return [_error("", "statement must be a JSON object")]

    errors: list[dict] = []
    _require_text(payload, "accountId", errors)
    _require_match(payload, "maskedAccountNumber", MASKED_ACCOUNT_NUMBER_PATTERN, errors)
    _require_text(payload, "currency", errors)
    _require_amount(payload, "openingBalance", errors)
    _require_amount(payload, "closingBalance", errors)
    _require_integer(payload, "largeTransactionCount", errors)

    period = payload.get("period")
    if isinstance(period, dict):
        _require_text(period, "from", errors, "period.")
        _require_text(period, "to", errors, "period.")
    else:
        errors.append(_error("period", "is required and must be an object"))

    totals = payload.get("totals")
    if isinstance(totals, dict):
        _require_integer(totals, "creditCount", errors, "totals.")
        _require_integer(totals, "debitCount", errors, "totals.")
        _require_amount(totals, "credits", errors, "totals.")
        _require_amount(totals, "debits", errors, "totals.")
    else:
        errors.append(_error("totals", "is required and must be an object"))

    entries = payload.get("transactions")
    if isinstance(entries, list):
        for index, entry in enumerate(entries):
            at = f"transactions[{index}]"
            if not isinstance(entry, dict):
                errors.append(_error(at, "must be an object"))
                continue
            _require_integer(entry, "id", errors, at + ".")
            _require_text(entry, "date", errors, at + ".")
            _require_text(entry, "description", errors, at + ".")
            _require_amount(entry, "amount", errors, at + ".")
            _require_amount(entry, "runningBalance", errors, at + ".")
            _require_boolean(entry, "largeTransaction", errors, at + ".")
    else:
        errors.append(_error("transactions", "is required and must be an array"))

    return errors


def _error(field: str, message: str) -> dict:
    return {"field": field, "message": message}


def _require(container, name, is_valid, message, errors, prefix=""):
    if name not in container:
        errors.append(_error(prefix + name, "is required"))
    elif not is_valid(container[name]):
        errors.append(_error(prefix + name, message))


def _require_text(container, name, errors, prefix=""):
    _require(
        container, name,
        lambda v: isinstance(v, str) and v != "",
        "must be a non-empty string", errors, prefix,
    )


def _require_amount(container, name, errors, prefix=""):
    _require_match(container, name, AMOUNT_PATTERN, errors, prefix)


def _require_match(container, name, pattern, errors, prefix=""):
    _require(
        container, name,
        lambda v: isinstance(v, str) and pattern.match(v) is not None,
        f"must be a string matching {pattern.pattern}", errors, prefix,
    )


def _require_integer(container, name, errors, prefix=""):
    _require(
        container, name,
        lambda v: isinstance(v, int) and not isinstance(v, bool),
        "must be an integer", errors, prefix,
    )


def _require_boolean(container, name, errors, prefix=""):
    _require(
        container, name, lambda v: isinstance(v, bool), "must be a boolean", errors, prefix
    )
