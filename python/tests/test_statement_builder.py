"""The golden test for Part C.

It reads fixtures, calls build_statement, and compares the result with a
statement document that is known to be correct. It never touches the API, so
it runs with the server switched off.

This test fails on a fresh clone because build_statement is not written yet.
Do not change this test.
"""

from integration.statement_builder import build_statement


def test_build_statement_matches_the_golden_statement(load_fixture):
    given = load_fixture("acc-1001-input.json")
    expected = load_fixture("acc-1001-statement.json")

    actual = build_statement(given["account"], given["transactions"], given["period"])

    assert actual == expected
