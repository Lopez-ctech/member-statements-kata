# Member Statements Kata

A small take-home exercise.

You are handed a working service you did not write: a **Core Accounts API** over a
bank ledger. Downstream of it, a nightly job builds monthly **member
statements** and delivers them to a statement service. You will read the API,
fix a defect in it, and build the statement job.

## 1. Pick one language

Java or Python — whichever you are more comfortable in. The exercise is
identical in both: same endpoints, same data, same defect, same fixtures. Do not
do both.

| | Java | Python |
|---|---|---|
| Stack | Spring Boot 3, H2 in-memory, JUnit 5 | FastAPI, SQLite in-memory, pytest |
| You need | Temurin **JDK 17** | Python **3.10+** |
| macOS | `brew install --cask temurin@17` | `brew install python` or [python.org](https://www.python.org/downloads/) |
| Linux | `sudo apt install openjdk-17-jdk` (or `dnf install java-17-openjdk-devel`) | `sudo apt install python3 python3-venv` |
| Windows | [Temurin 17 MSI](https://adoptium.net/temurin/releases/?version=17) — tick **"Set JAVA_HOME variable"** | [python.org installer](https://www.python.org/downloads/) — tick **"Add python.exe to PATH"** |

Maven is **not** a prerequisite: the repository ships the Maven wrapper
(`java/mvnw`), which downloads what it needs on first use.

> **If your machine fights you**, open the repository in **GitHub Codespaces**
> (green *Code* button → *Codespaces* → *Create codespace*). Java 17 and Python
> are already installed there. Do not spend an hour on local setup.

## 2. Setup

One command. It installs dependencies, runs the tests once, and tells you which
failures are supposed to be there.

**macOS / Linux**

```bash
scripts/setup.sh python      # or: scripts/setup.sh java
```

**Windows (PowerShell)**

```powershell
powershell -ExecutionPolicy Bypass -File scripts\setup.ps1 python
```

The `-ExecutionPolicy Bypass` is there because PowerShell blocks unsigned scripts
by default. The first Java setup downloads Maven and the Spring dependencies and
can take **2 to 5 minutes** — longer on Windows, where Defender scans every jar.

You should see this at the end:

```
API tests:          8 passed, 1 failed     (expected: that failure is Part B)
Integration tests:  1 failed               (expected: that failure is Part C)
```

Both failures are meant to be there. If you see something else — a stack trace at
startup, no tests found, a missing interpreter — that is a setup problem, not the
exercise. Say so when you submit.

### Start the API

```bash
scripts/run.sh python
```
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 python
```

Then, in another terminal:

```bash
curl http://localhost:8080/health
```

You should get `{"status":"UP"}`. Port 8080 already taken? Set `PORT` first — the
run scripts and the end-to-end check both honour it.

### What is where

```
docs/api-contract.yaml       the OpenAPI contract for the API
docs/statement-schema.json   the JSON Schema for the document you will build
docs/data-dictionary.md      the tables, the cents convention, the UTC convention
data/seed.sql                the schema and data, shared by both implementations
fixtures/                    the input and the known-good output for Part C
java/ | python/              the two implementations; work in one of them
scripts/                     setup, run, and the end-to-end check
```

---

## 3. Part A — read the code

Before changing anything, read it.

In `SUBMISSION.md`, describe in **3 to 5 sentences** what the service does and how
a request flows through it, from the moment it arrives to the moment JSON comes
back. Name the layers and the files.

A good place to start: `GET /accounts/ACC-1001/transactions?from=2025-01-01&to=2025-01-31`.

You will be asked to walk through this live, so write what you actually
understand rather than what sounds thorough.

---

## 4. Part B — fix the failing test

Run the tests. One of them fails.

```bash
cd python && .venv/bin/python -m pytest        # .venv/Scripts/python.exe on Windows
cd java && ./mvnw test                         # .\mvnw.cmd test on Windows
```

The failing test is the ticket. It describes real, incorrect behaviour: the API
is not returning what its own contract promises.

- **Fix the code so the test passes.**
- **Do not change the test**, and do not change the contract in
  `docs/api-contract.yaml` to match the bug. The contract and the test agree with
  each other; the code is what disagrees.
- Read the failure message carefully before you start. Then read it again after
  your first attempt — the obvious one-character fix is not enough here.
- In `SUBMISSION.md`, say what was wrong and why your fix is right.

Exactly one test should fail before your change, and none after.

---

## 5. Part C — build the statement builder

Write the job that turns an account and its transactions into a **member
statement** and delivers it.

Your code goes here:

| | Java | Python |
|---|---|---|
| Write this function | `java/src/main/java/com/cu/integration/StatementBuilder.java` | `python/integration/statement_builder.py` |
| Already wired for you | `StatementRunner.java` | the same file: `fetch_inputs`, `deliver`, `run_statement` |
| Golden test (do not change) | `src/test/java/com/cu/integration/StatementBuilderTest.java` | `tests/test_statement_builder.py` |
| Your own tests go here | `src/test/java/com/cu/integration/MyTests.java` | `tests/test_my_tests.py` |

The job has three steps. Steps 1 and 3 are written for you:

1. **Fetch** — `GET /accounts/{id}` and `GET /accounts/{id}/transactions` for the period.
2. **Transform** — `build_statement` / `buildStatement`. **This is the part you write.**
3. **Deliver** — `POST /statements`, then print the returned `statementId`.

Keep step 2 **pure**: same inputs, same output, no HTTP, no file access, no clock.
That is what lets its tests run with the API switched off, and you will be asked
to demonstrate exactly that.

### The statement document

```json
{
  "accountId": "ACC-1001",
  "maskedAccountNumber": "****4821",
  "currency": "USD",
  "period": { "from": "2025-01-01", "to": "2025-01-31" },
  "openingBalance": "1520.00",
  "closingBalance": "13808.00",
  "totals": { "creditCount": 2, "debitCount": 4, "credits": "12512.50", "debits": "224.50" },
  "transactions": [
    { "id": 17, "date": "2025-01-03", "description": "POS DEBIT COFFEE CO",
      "amount": "-45.00", "runningBalance": "1475.00", "largeTransaction": false }
  ],
  "largeTransactionCount": 1
}
```

The full schema is `docs/statement-schema.json`, and
`fixtures/acc-1001-statement.json` is a complete, known-good example.

### The rules

- **Every money value is a string with exactly two decimals**, converted from the
  integer cents the API returns. Floating point is not acceptable anywhere in the
  money path — not in the arithmetic, not in the formatting.
- **Debits are negative, credits positive.** The sign is derived from `type`;
  `amountCents` from the API is always positive.
- **Sort transactions by `postedAt` ascending, then by `id` ascending.** The
  `date` field is the UTC calendar date of `postedAt`.
- **`runningBalance` starts from `openingBalance`** and accumulates in that sorted
  order. `closingBalance` is the last running balance — or the opening balance if
  the period has no transactions.
- **`maskedAccountNumber` is four asterisks plus the last four digits.** Decide
  and document what you do with a number shorter than four digits.
- **`largeTransaction` is true when the absolute amount is strictly greater than
  10,000.00.** Exactly 10,000.00 is not flagged. Put the threshold in a single
  named constant or config value, not inline in a comparison.
- **An account with no transactions in the period still produces a valid
  statement.**

Not in scope: authentication, pagination, currency conversion, retries, Docker.

### Checking your work

The golden test feeds `fixtures/acc-1001-input.json` into your function and
compares the result with `fixtures/acc-1001-statement.json`. It never touches the
API, so it passes even if Part B is still broken.

The end-to-end check does touch the API. Start the API in one terminal, then:

```bash
scripts/check-e2e.sh python
```
```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-e2e.ps1 python
```

It builds the January 2025 statement for ACC-1001 against the running API, POSTs
it, and compares the result with the golden file. If transactions are missing
here but the golden test passes, look at Part B again.

### Add at least two tests of your own

Put them in the placeholder test file. Pick cases you think are worth pinning
down and be ready to say why you chose those. Some possibilities: an empty
period, the 10,000.00 boundary, an account number shorter than four digits, two
transactions with the same `postedAt`.

### If you finish early (optional)

Make the builder handle a `404` from the API with a clear error message and a
non-zero exit code. One improvement, not a list.

---

## 6. Submitting

1. Create your own **private** repository from this template — green *Use this
   template* button, then *Create a new repository*, and set it to **Private**.
   Please do not fork: forks of a public repository are public.
2. **Commit as you go.** Your commit history is part of what we look at; a single
   "done" commit tells us nothing.
3. Add the interviewer as a **collaborator** on your private repo. The username is
   in your invitation email.
4. Fill in `SUBMISSION.md`. It is short and it matters — we read it before the
   session and compare it with what you say live.

The GitHub Actions workflow runs both test suites on every push. On a fresh clone
both jobs are red, by design. By the time you submit, the job for **your**
language should be green; leave the other one red.

## 7. What happens next

A 45 to 60 minute conversation. You will share your screen and walk through the
code, explain the defect and your fix, talk through your statement builder and
your tests, and make one small change to it live while we watch.

We are not testing recall. Bring the repo, bring your reasoning, and be ready to
say "I do not know" about the parts you did not get to.
