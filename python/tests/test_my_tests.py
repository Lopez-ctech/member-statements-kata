"""Your tests go here. Add at least two.

The golden test in test_statement_builder.py proves one account works for one
period. It does not prove much else. Pick cases you think are worth pinning
down and say why you chose them in SUBMISSION.md.

Some candidates worth considering (you do not have to use these):

  * an account with no transactions in the period
  * an amount of exactly 10,000.00 at the largeTransaction boundary
  * an account number shorter than four digits
  * two transactions with the same postedAt

You can build inputs by hand; nothing here needs the API to be running.
A test looks like this:

    from integration.statement_builder import build_statement

    def test_empty_period_still_produces_a_statement():
        account = {
            "id": "ACC-9001", "accountNumber": "999888777666", "currency": "USD",
            "openingBalanceCents": 50000, "currentBalanceCents": 50000,
        }
        statement = build_statement(account, [], {"from": "2025-03-01", "to": "2025-03-31"})
        assert statement["closingBalance"] == "500.00"
        assert statement["transactions"] == []
"""
