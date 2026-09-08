# Start the Core Accounts API. Listens on $env:PORT, or 8080 if that is not set.
#
#   powershell -ExecutionPolicy Bypass -File scripts\run.ps1 python
#   $env:PORT = 9000; powershell -ExecutionPolicy Bypass -File scripts\run.ps1 java
#
# Check it with:  curl http://localhost:8080/health

param([string]$Stack = '')

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $env:PORT) { $env:PORT = '8080' }

if ($Stack -ne 'java' -and $Stack -ne 'python') {
    Write-Host 'usage: powershell -ExecutionPolicy Bypass -File scripts\run.ps1 <java|python>'
    Write-Host '       (honours the PORT environment variable)'
    exit 2
}

Write-Host "Starting the Core Accounts API on http://localhost:$($env:PORT)  (Ctrl-C to stop)"

if ($Stack -eq 'python') {
    Set-Location (Join-Path $repoRoot 'python')
    $venvPython = Join-Path (Get-Location) '.venv\Scripts\python.exe'
    if (-not (Test-Path $venvPython)) {
        Write-Host 'error: python\.venv is missing. Run scripts\setup.ps1 python first.' -ForegroundColor Red
        exit 1
    }
    & $venvPython -m uvicorn app.main:app --port $env:PORT
}
else {
    Set-Location (Join-Path $repoRoot 'java')
    & .\mvnw.cmd -q spring-boot:run
}
