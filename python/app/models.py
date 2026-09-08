"""Domain types and their JSON representations.

The database speaks snake_case; the API speaks camelCase. The translation
between the two lives here, in one place.

Money never leaves this module as a float. Amounts are integer cents and the
JSON field names carry a `Cents` suffix so nobody mistakes them for dollars.
"""

from dataclasses import dataclass


@dataclass(frozen=True)
class Account:
    id: str
    member_id: str
    account_number: str
    type: str
    currency: str
    status: str
    opening_balance_cents: int

    @staticmethod
    def from_row(row) -> "Account":
        return Account(
            id=row["id"],
            member_id=row["member_id"],
            account_number=row["account_number"],
            type=row["type"],
            currency=row["currency"],
            status=row["status"],
            opening_balance_cents=int(row["opening_balance_cents"]),
        )

    def to_summary_json(self) -> dict:
        """Shape used by GET /accounts. Deliberately carries no balances."""
        return {
            "id": self.id,
            "memberId": self.member_id,
            "accountNumber": self.account_number,
            "type": self.type,
            "currency": self.currency,
            "status": self.status,
        }

    def to_detail_json(self, current_balance_cents: int) -> dict:
        """Shape used by GET /accounts/{id}. The current balance is computed."""
        return {
            **self.to_summary_json(),
            "openingBalanceCents": self.opening_balance_cents,
            "currentBalanceCents": current_balance_cents,
        }


@dataclass(frozen=True)
class Transaction:
    id: int
    account_id: str
    posted_at: str
    type: str
    amount_cents: int
    description: str

    @staticmethod
    def from_row(row) -> "Transaction":
        return Transaction(
            id=int(row["id"]),
            account_id=row["account_id"],
            posted_at=row["posted_at"],
            type=row["type"],
            amount_cents=int(row["amount_cents"]),
            description=row["description"],
        )

    def to_json(self) -> dict:
        return {
            "id": self.id,
            "accountId": self.account_id,
            "postedAt": self.posted_at,
            "type": self.type,
            "amountCents": self.amount_cents,
            "description": self.description,
        }
