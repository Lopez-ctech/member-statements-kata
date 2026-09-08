# End-to-end check for Part C. The API must already be running in another
# terminal (scripts\run.ps1 <stack>).
#
#   powershell -ExecutionPolicy Bypass -File scripts\check-e2e.ps1 python
#   powershell -ExecutionPolicy Bypass -File scripts\check-e2e.ps1 java
#
# It runs your statement builder against the LIVE API for ACC-1001 over
# January 2025, POSTs the result to /statements, and compares what you built
# with fixtures\acc-1001-statement.json.
#
# This is the check the unit test cannot do: the unit test feeds you fixture
# data, so it passes even if the API is still returning the wrong transactions.
# This one goes through the API, so it fails if Part B is unfixed.

param([string]$Stack = '')

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $env:PORT) { $env:PORT = '8080' }
$baseUrl = $env:API_BASE_URL
if (-not $baseUrl) { $baseUrl = "http://localhost:$($env:PORT)" }
# Relative to python\ and to java\, which is where each branch below runs.
# Kept relative so the two scripts behave identically; Git Bash on Windows
# rewrites absolute POSIX paths handed to a native program.
$golden = '../fixtures/acc-1001-statement.json'

if ($Stack -ne 'java' -and $Stack -ne 'python') {
    Write-Host 'usage: powershell -ExecutionPolicy Bypass -File scripts\check-e2e.ps1 <java|python>'
    exit 2
}

Write-Host "Checking $baseUrl ..."
try {
    Invoke-WebRequest -Uri "$baseUrl/health" -UseBasicParsing -TimeoutSec 10 | Out-Null
}
catch {
    Write-Host "error: the API is not answering at $baseUrl" -ForegroundColor Red
    Write-Host "  start it in another terminal with: powershell -ExecutionPolicy Bypass -File scripts\run.ps1 $Stack"
    exit 1
}

if ($Stack -eq 'python') {
    Set-Location (Join-Path $repoRoot 'python')
    $venvPython = Join-Path (Get-Location) '.venv\Scripts\python.exe'
    & $venvPython -m integration.statement_builder `
        ACC-1001 2025-01-01 2025-01-31 --base-url $baseUrl --expect $golden
    $status = $LASTEXITCODE
}
else {
    Set-Location (Join-Path $repoRoot 'java')
    & .\mvnw.cmd -q compile exec:java `
        "-Dexec.args=ACC-1001 2025-01-01 2025-01-31 --base-url $baseUrl --expect $golden"
    $status = $LASTEXITCODE
}

Write-Host ''
if ($status -eq 0) {
    Write-Host 'PASS  end-to-end: the statement built from the live API matches the golden file.' -ForegroundColor Green
}
else {
    Write-Host 'FAIL  end-to-end: see the differences above.' -ForegroundColor Red
    Write-Host '      If transactions are missing, check Part B before looking at Part C.'
}
exit $status
