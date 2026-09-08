"""Tests for the accounts API.

One of these fails on a fresh clone. That failure is Part B of the exercise:
it is a real defect in the application code, not a broken test. Fix the code,
not the test.
"""


def test_health_reports_up(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}


def test_accounts_are_listed_without_balances(client):
    response = client.get("/accounts")
    assert response.status_code == 200

    accounts = response.json()
    assert [account["id"] for account in accounts] == [
        "ACC-1001", "ACC-1002", "ACC-1003", "ACC-1004", "ACC-1005",
    ]
    assert accounts[0]["accountNumber"] == "483920174821"
    assert accounts[0]["currency"] == "USD"
    for account in accounts:
        assert "openingBalanceCents" not in account
        assert "currentBalanceCents" not in account


def test_account_detail_includes_opening_and_current_balance(client):
    response = client.get("/accounts/ACC-1001")
    assert response.status_code == 200

    account = response.json()
    assert account["id"] == "ACC-1001"
    assert account["memberId"] == "MEM-1"
    assert account["type"] == "CHECKING"
    assert account["status"] == "OPEN"
    # Opening balance is stored; the current balance is computed from every
    # transaction on the account, with no date filter.
    assert account["openingBalanceCents"] == 152000
    assert account["currentBalanceCents"] == 1395700


def test_unknown_account_returns_404(client):
    response = client.get("/accounts/ACC-9999")
    assert response.status_code == 404
    assert response.json()["error"] == "NOT_FOUND"

    ranged = client.get(
        "/accounts/ACC-9999/transactions",
        params={"from": "2025-01-01", "to": "2025-01-31"},
    )
    assert ranged.status_code == 404


def test_transactions_require_from_and_to(client):
    assert client.get("/accounts/ACC-1001/transactions").status_code == 400
    assert client.get(
        "/accounts/ACC-1001/transactions", params={"from": "2025-01-01"}
    ).status_code == 400
    assert client.get(
        "/accounts/ACC-1001/transactions", params={"to": "2025-01-31"}
    ).status_code == 400
    assert client.get(
        "/accounts/ACC-1001/transactions",
        params={"from": "01/01/2025", "to": "2025-01-31"},
    ).status_code == 400


def test_transactions_reject_from_after_to(client):
    response = client.get(
        "/accounts/ACC-1001/transactions",
        params={"from": "2025-01-31", "to": "2025-01-01"},
    )
    assert response.status_code == 400
    assert response.json()["error"] == "BAD_REQUEST"


def test_transactions_include_last_day_of_range(client):
    """The date range is inclusive on BOTH ends.

    docs/api-contract.yaml and the README both say so. ACC-1001 has six
    transactions in January 2025, the last of them posted on the 31st.
    """
    response = client.get(
        "/accounts/ACC-1001/transactions",
        params={"from": "2025-01-01", "to": "2025-01-31"},
    )
    assert response.status_code == 200

    returned_ids = [transaction["id"] for transaction in response.json()]
    # Ordered by postedAt, then id.
    expected_ids = [17, 37, 29, 26, 8, 3]
    missing = [id_ for id_ in expected_ids if id_ not in returned_ids]

    assert returned_ids == expected_ids, (
        "ACC-1001 from 2025-01-01 to 2025-01-31: expected "
        f"{len(expected_ids)} transactions, got {len(returned_ids)}; "
        f"missing id(s) {missing}. Transaction id 3 is posted at "
        "2025-01-31T14:30:00Z, CREDIT 1250000, 'ACH CREDIT PAYROLL ACME CORP'."
    )


def test_statement_is_accepted_and_returns_an_id(client, valid_statement):
    response = client.post("/statements", json=valid_statement)
    assert response.status_code == 201
    assert response.json()["statementId"].startswith("STMT-")


def test_statement_with_bad_fields_returns_422(client, valid_statement):
    broken = dict(valid_statement)
    broken["maskedAccountNumber"] = "4821"       # missing the four asterisks
    broken["openingBalance"] = 1520.0            # a float, not a "0.00" string
    broken["totals"] = dict(valid_statement["totals"], credits="12512.5")
    del broken["currency"]

    response = client.post("/statements", json=broken)
    assert response.status_code == 422

    body = response.json()
    assert body["error"] == "UNPROCESSABLE_ENTITY"
    fields = {error["field"] for error in body["fieldErrors"]}
    assert fields == {
        "maskedAccountNumber", "openingBalance", "totals.credits", "currency",
    }
