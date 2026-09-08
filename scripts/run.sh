#!/usr/bin/env bash
#
# Start the Core Accounts API. Listens on $PORT, or 8080 if that is not set.
#
#   scripts/run.sh python
#   PORT=9000 scripts/run.sh java
#
# Check it with:  curl http://localhost:8080/health

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STACK="${1:-}"
PORT="${PORT:-8080}"

if [ "$STACK" != "java" ] && [ "$STACK" != "python" ]; then
    echo "usage: scripts/run.sh <java|python>     (honours the PORT variable)" >&2
    exit 2
fi

echo "Starting the Core Accounts API on http://localhost:$PORT  (Ctrl-C to stop)"

if [ "$STACK" = "python" ]; then
    cd "$REPO_ROOT/python" || exit 1
    VENV_PYTHON=".venv/bin/python"
    [ -x "$VENV_PYTHON" ] || VENV_PYTHON=".venv/Scripts/python.exe"
    if [ ! -x "$VENV_PYTHON" ]; then
        echo "error: python/.venv is missing. Run scripts/setup.sh python first." >&2
        exit 1
    fi
    exec "$VENV_PYTHON" -m uvicorn app.main:app --port "$PORT"
else
    cd "$REPO_ROOT/java" || exit 1
    export PORT
    exec ./mvnw -q spring-boot:run
fi
