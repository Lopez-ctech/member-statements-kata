# Data dictionary

The ledger behind the Core Accounts API. Three tables, defined and populated by
[`data/seed.sql`](../data/seed.sql), which both the Java and the Python
implementations load into an in-memory database at startup.

All of the data is synthetic.

## Conventions

These three hold everywhere — in the database, in the API, and in the statement
document.

**Money is an integer number of cents.** `1250000` is twelve thousand five
hundred dollars. There is no floating point anywhere in the money path, because
binary floating point cannot represent most decimal fractions exactly: `0.1 +
0.2` is `0.30000000000000004`, and a ledger that is off by a fraction of a cent
per transaction is a ledger that does not reconcile. Database columns and API
fields that carry money are named with a `Cents` suffix so there is no doubt
about the unit. The statement document is the one place amounts become strings
(`"12500.00"`), never JSON numbers — see
[`statement-schema.json`](statement-schema.json).

**Amounts are always positive. The sign comes from `type`.** A row never holds
`-4500`. It holds `4500` with `type = 'DEBIT'`. Anything that needs a signed
number derives it in one place from the type.

**Timestamps are UTC.** `posted_at` is an ISO 8601 string ending in `Z`, for
example `2025-01-31T14:30:00Z`. The credit union operates in Puerto Rico, which
is AST (UTC−4) year round, so the UTC date and the local date are not always the
same date.

## `members`

A credit union has members, not customers. The distinction is not cosmetic —
members are the owners.

| Column | Type | Notes |
|---|---|---|
| `id` | `VARCHAR(16)` | Primary key. `MEM-1`, `MEM-2`, `MEM-3`. |
| `full_name` | `VARCHAR(120)` | |
| `email` | `VARCHAR(160)` | |

Three rows.

## `accounts`

| Column | Type | Notes |
|---|---|---|
| `id` | `VARCHAR(16)` | Primary key. `ACC-1001` … `ACC-1005`. |
| `member_id` | `VARCHAR(16)` | References `members.id`. A member may hold several accounts. |
| `account_number` | `VARCHAR(12)` | Twelve digits. Never shown in full on a statement. |
| `type` | `VARCHAR(16)` | `CHECKING` or `SAVINGS`, enforced by a `CHECK` constraint. |
| `currency` | `VARCHAR(3)` | ISO 4217. Mostly `USD`; one account is `EUR`. |
| `status` | `VARCHAR(16)` | `OPEN` or `CLOSED`, enforced by a `CHECK` constraint. |
| `opening_balance_cents` | `BIGINT` | The balance as of `2025-01-01T00:00:00Z`, before any transaction in this table. Stored. |

Five rows.

There is no stored current balance. `currentBalanceCents` is computed on every
read as `opening_balance_cents` plus every credit minus every debit, over all
transactions on the account with no date filter. Computing it means it can never
drift out of step with the transactions it comes from; the cost is that the work
is redone on every request. Real core banking systems store a balance and
reconcile it against the transaction history on a schedule, which is a different
trade: fast reads, and a reconciliation job you have to get right.

## `transactions`

| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER` | Primary key. Assigned in the seed, not sequential in time. |
| `account_id` | `VARCHAR(16)` | References `accounts.id`. |
| `posted_at` | `VARCHAR(20)` | ISO 8601 UTC, e.g. `2025-01-31T14:30:00Z`. Stored as text, which sorts and compares lexicographically in the same order as chronologically. |
| `type` | `VARCHAR(16)` | `DEBIT` or `CREDIT`, enforced by a `CHECK` constraint. |
| `amount_cents` | `BIGINT` | Always greater than zero, enforced by a `CHECK` constraint. |
| `description` | `VARCHAR(120)` | As it appears on a statement, e.g. `ACH CREDIT PAYROLL ACME CORP`. |

Forty rows, spanning December 2024 to February 2025.

**Row order is not time order.** Ids were not assigned chronologically, so a
query with no `ORDER BY` returns transactions in an order that has nothing to do
with when they were posted. Anything that depends on chronology — a running
balance, for instance — has to sort explicitly.

**Two transactions can share a `posted_at`.** Timestamps are not unique, so
sorting on `posted_at` alone is ambiguous. Queries here break the tie on `id`.
