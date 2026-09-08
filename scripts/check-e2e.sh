#!/usr/bin/env bash
#
# End-to-end check for Part C. The API must already be running in another
# terminal (scripts/run.sh <stack>).
#
#   scripts/check-e2e.sh python
#   scripts/check-e2e.sh java
#
# It runs your statement builder against the LIVE API for ACC-1001 over
# January 2025, POSTs the result to /statements, and compares what you built
# with fixtures/acc-1001-statement.json.
#
# This is the check the unit test cannot do: the unit test feeds you fixture
# data, so it passes even if the API is still returning the wrong transactions.
# This one goes through the API, so it fails if Part B is unfixed.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STACK="${1:-}"
PORT="${PORT:-8080}"
BASE_URL="${API_BASE_URL:-http://localhost:$PORT}"

# Relative to python/ and to java/, which is where each branch below runs.
# Deliberately not absolute: Git Bash on Windows rewrites absolute POSIX paths
# when it hands them to a native program, and mangles them inside a quoted
# -Dexec.args=... string.
GOLDEN="../fixtures/acc-1001-statement.json"

if [ "$STACK" != "java" ] && [ "$STACK" != "python" ]; then
    echo "usage: scripts/check-e2e.sh <java|python>" >&2
    exit 2
fi

echo "Checking $BASE_URL ..."
if ! curl -fsS "$BASE_URL/health" >/dev/null 2>&1; then
    echo "error: the API is not answering at $BASE_URL" >&2
    echo "  start it in another terminal with: scripts/run.sh $STACK" >&2
    exit 1
fi

if [ "$STACK" = "python" ]; then
    cd "$REPO_ROOT/python" || exit 1
    VENV_PYTHON=".venv/bin/python"
    [ -x "$VENV_PYTHON" ] || VENV_PYTHON=".venv/Scripts/python.exe"
    "$VENV_PYTHON" -m integration.statement_builder \
        ACC-1001 2025-01-01 2025-01-31 \
        --base-url "$BASE_URL" \
        --expect "$GOLDEN"
    status=$?
else
    cd "$REPO_ROOT/java" || exit 1
    ./mvnw -q compile exec:java \
        -Dexec.args="ACC-1001 2025-01-01 2025-01-31 --base-url $BASE_URL --expect $GOLDEN"
    status=$?
fi

echo
if [ "$status" -eq 0 ]; then
    echo "PASS  end-to-end: the statement built from the live API matches the golden file."
else
    echo "FAIL  end-to-end: see the differences above."
    echo "      If transactions are missing, check Part B before looking at Part C."
fi
exit "$status"
