"""HTTP layer.

Reads parameters off the request, hands them to the service, and shapes the
response. No SQL, no arithmetic. The service raises domain errors; main.py
turns those into status codes.
"""

from fastapi import APIRouter, Query, Request

from app import services

router = APIRouter()


@router.get("/health")
def health() -> dict:
    return {"status": "UP"}


@router.get("/accounts")
def list_accounts() -> list[dict]:
    return [account.to_summary_json() for account in services.list_accounts()]


@router.get("/accounts/{account_id}")
def get_account(account_id: str) -> dict:
    account = services.get_account(account_id)
    return account.to_detail_json(services.current_balance_cents(account))


@router.get("/accounts/{account_id}/transactions")
def get_transactions(
    account_id: str,
    # `from` is a Python keyword, hence the alias.
    from_: str | None = Query(default=None, alias="from"),
    to: str | None = Query(default=None),
) -> list[dict]:
    transactions = services.get_transactions(account_id, from_, to)
    return [transaction.to_json() for transaction in transactions]


@router.post("/statements", status_code=201)
async def post_statement(request: Request) -> dict:
    try:
        payload = await request.json()
    except ValueError:
        raise services.ValidationError(
            [{"field": "", "message": "body must be valid JSON"}]
        ) from None
    return {"statementId": services.record_statement(payload)}
