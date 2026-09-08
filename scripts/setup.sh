#!/usr/bin/env bash
#
# One-time setup for the kata. Safe to run more than once.
#
#   scripts/setup.sh python
#   scripts/setup.sh java
#
# It installs dependencies, runs both test suites once, and tells you which
# failures are supposed to be there.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STACK="${1:-}"

usage() {
    cat <<'EOF'
usage: scripts/setup.sh <java|python>

  java     Spring Boot 3 + H2, built with the bundled Maven wrapper
  python   FastAPI + SQLite, installed into python/.venv

Pick whichever language you are more comfortable in. The exercise is the same
in both.
EOF
}

if [ "$STACK" != "java" ] && [ "$STACK" != "python" ]; then
    if [ -n "$STACK" ]; then
        echo "error: unknown argument '$STACK'" >&2
        echo >&2
    fi
    usage >&2
    exit 2
fi

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

find_python() {
    for candidate in python3 python; do
        if command -v "$candidate" >/dev/null 2>&1 \
            && "$candidate" -c 'import sys; sys.exit(0 if sys.version_info >= (3, 10) else 1)' \
                >/dev/null 2>&1; then
            echo "$candidate"
            return 0
        fi
    done
    return 1
}

# "1 failed, 8 passed, 1 warning in 0.14s"  ->  "8 passed, 1 failed"
summarise_pytest() {
    local line passed failed errors summary=""
    line="$(printf '%s\n' "$1" | grep -E '[0-9]+ (passed|failed|error)' | tail -1)"
    [ -n "$line" ] || return 0
    passed="$(printf '%s' "$line" | grep -oE '[0-9]+ passed' | grep -oE '^[0-9]+')"
    failed="$(printf '%s' "$line" | grep -oE '[0-9]+ failed' | grep -oE '^[0-9]+')"
    errors="$(printf '%s' "$line" | grep -oE '[0-9]+ errors?' | grep -oE '^[0-9]+')"
    [ -n "$passed" ] && summary="$passed passed"
    [ -n "$failed" ] && summary="${summary:+$summary, }$failed failed"
    [ -n "$errors" ] && summary="${summary:+$summary, }$errors errored"
    echo "$summary"
}

# "Tests run: 9, Failures: 1, Errors: 0, ..."  ->  "8 passed, 1 failed"
summarise_surefire() {
    local report="$1" line run failures errors bad
    [ -f "$report" ] || return 0
    line="$(grep -m1 'Tests run:' "$report")"
    run="$(printf '%s' "$line" | sed -n 's/.*Tests run: \([0-9]*\).*/\1/p')"
    failures="$(printf '%s' "$line" | sed -n 's/.*Failures: \([0-9]*\).*/\1/p')"
    errors="$(printf '%s' "$line" | sed -n 's/.*Errors: \([0-9]*\).*/\1/p')"
    [ -n "$run" ] || return 0
    bad=$(( failures + errors ))
    local passed=$(( run - bad )) summary=""
    [ "$passed" -gt 0 ] && summary="$passed passed"
    [ "$bad" -gt 0 ] && summary="${summary:+$summary, }$bad failed"
    echo "${summary:-0 tests}"
}

# ---------------------------------------------------------------------------
# Install, build, test
# ---------------------------------------------------------------------------

api_line=""
integration_line=""
log_hint=""

if [ "$STACK" = "python" ]; then
    if ! PYTHON="$(find_python)"; then
        echo "error: Python 3.10 or newer was not found on your PATH." >&2
        echo "  install it from https://www.python.org/downloads/ (tick 'Add to PATH')" >&2
        exit 1
    fi
    echo "Using $PYTHON ($("$PYTHON" --version 2>&1))"

    cd "$REPO_ROOT/python" || exit 1
    if [ ! -d .venv ]; then
        echo "Creating python/.venv ..."
        "$PYTHON" -m venv .venv || exit 1
    fi

    # Git Bash on Windows puts the interpreter in Scripts/, not bin/.
    VENV_PYTHON=".venv/bin/python"
    [ -x "$VENV_PYTHON" ] || VENV_PYTHON=".venv/Scripts/python.exe"
    if [ ! -x "$VENV_PYTHON" ]; then
        echo "error: python/.venv looks broken. Delete it and run this script again." >&2
        exit 1
    fi

    echo "Installing dependencies ..."
    "$VENV_PYTHON" -m pip install --quiet --disable-pip-version-check -r requirements.txt || exit 1

    echo "Running the tests ..."
    api_line="$(summarise_pytest "$("$VENV_PYTHON" -m pytest tests/test_api.py 2>&1)")"
    integration_line="$(summarise_pytest \
        "$("$VENV_PYTHON" -m pytest tests/test_statement_builder.py tests/test_my_tests.py 2>&1)")"
    log_hint="Re-run them any time with:  cd python && $VENV_PYTHON -m pytest"
else
    if ! command -v java >/dev/null 2>&1; then
        echo "error: java was not found on your PATH." >&2
        echo "  install Temurin JDK 17 from https://adoptium.net/temurin/releases/?version=17" >&2
        exit 1
    fi
    echo "Using $(java -version 2>&1 | head -1)"

    cd "$REPO_ROOT/java" || exit 1
    echo "Resolving dependencies (the first run downloads Maven and can take 2-5 minutes) ..."
    ./mvnw -B -q dependency:resolve || exit 1

    echo "Building and running the tests ..."
    mkdir -p target
    ./mvnw -B test -Dmaven.test.failure.ignore=true > target/setup-tests.log 2>&1
    api_line="$(summarise_surefire target/surefire-reports/com.cu.api.AccountsApiTest.txt)"
    integration_line="$(summarise_surefire \
        target/surefire-reports/com.cu.integration.StatementBuilderTest.txt)"
    log_hint="Full output is in java/target/setup-tests.log; re-run with:  cd java && ./mvnw test"
fi

echo
echo "--------------------------------------------------------------------"
printf 'API tests:          %-21s (expected: that failure is Part B)\n' \
    "${api_line:-could not read results}"
printf 'Integration tests:  %-21s (expected: that failure is Part C)\n' \
    "${integration_line:-could not read results}"
echo
echo "Start the API with: scripts/run.sh $STACK"
echo "$log_hint"
echo "--------------------------------------------------------------------"
