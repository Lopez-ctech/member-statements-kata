# One-time setup for the kata. Safe to run more than once.
#
#   powershell -ExecutionPolicy Bypass -File scripts\setup.ps1 python
#   powershell -ExecutionPolicy Bypass -File scripts\setup.ps1 java
#
# It installs dependencies, runs both test suites once, and tells you which
# failures are supposed to be there.

param([string]$Stack = '')

$repoRoot = Split-Path -Parent $PSScriptRoot

function Show-Usage {
    Write-Host @'
usage: powershell -ExecutionPolicy Bypass -File scripts\setup.ps1 <java|python>

  java     Spring Boot 3 + H2, built with the bundled Maven wrapper
  python   FastAPI + SQLite, installed into python\.venv

Pick whichever language you are more comfortable in. The exercise is the same
in both.
'@
}

if ($Stack -ne 'java' -and $Stack -ne 'python') {
    if ($Stack -ne '') { Write-Host "error: unknown argument '$Stack'`n" -ForegroundColor Red }
    Show-Usage
    exit 2
}

function Find-PythonCommand {
    # py -3 first: it is the launcher the python.org installer always registers.
    $candidates = @(
        @{ Exe = 'py';      Pre = @('-3') },
        @{ Exe = 'python';  Pre = @() },
        @{ Exe = 'python3'; Pre = @() }
    )
    foreach ($candidate in $candidates) {
        if (-not (Get-Command $candidate.Exe -ErrorAction SilentlyContinue)) { continue }
        $probe = $candidate.Pre + @('-c', 'import sys; sys.exit(0 if sys.version_info >= (3,10) else 1)')
        & $candidate.Exe @probe 2>$null
        if ($LASTEXITCODE -eq 0) { return $candidate }
    }
    return $null
}

# "1 failed, 8 passed, 1 warning in 0.14s"  ->  "8 passed, 1 failed"
function Get-PytestSummary([string]$Output) {
    $line = $Output -split "`n" |
        Where-Object { $_ -match '\d+ (passed|failed|error)' } |
        Select-Object -Last 1
    if (-not $line) { return '' }
    $parts = @()
    foreach ($pair in @(@('passed', 'passed'), @('failed', 'failed'), @('errors?', 'errored'))) {
        $found = [regex]::Match($line, "(\d+) $($pair[0])")
        if ($found.Success) { $parts += "$($found.Groups[1].Value) $($pair[1])" }
    }
    return ($parts -join ', ')
}

# "Tests run: 9, Failures: 1, Errors: 0, ..."  ->  "8 passed, 1 failed"
function Get-SurefireSummary([string]$Report) {
    if (-not (Test-Path $Report)) { return '' }
    $line = Get-Content $Report | Where-Object { $_ -match 'Tests run: (\d+), Failures: (\d+), Errors: (\d+)' } |
        Select-Object -First 1
    if (-not $line) { return '' }
    $null = $line -match 'Tests run: (\d+), Failures: (\d+), Errors: (\d+)'
    $run = [int]$Matches[1]
    $bad = [int]$Matches[2] + [int]$Matches[3]
    $parts = @()
    if (($run - $bad) -gt 0) { $parts += "$($run - $bad) passed" }
    if ($bad -gt 0) { $parts += "$bad failed" }
    if ($parts.Count -eq 0) { return '0 tests' }
    return ($parts -join ', ')
}

$apiLine = ''
$integrationLine = ''
$logHint = ''

if ($Stack -eq 'python') {
    $python = Find-PythonCommand
    if ($null -eq $python) {
        Write-Host 'error: Python 3.10 or newer was not found on your PATH.' -ForegroundColor Red
        Write-Host '  install it from https://www.python.org/downloads/ (tick "Add python.exe to PATH")'
        exit 1
    }
    $version = (& $python.Exe @($python.Pre + @('--version')))
    Write-Host "Using $($python.Exe) $($python.Pre -join ' ') ($version)"

    Set-Location (Join-Path $repoRoot 'python')
    if (-not (Test-Path '.venv')) {
        Write-Host 'Creating python\.venv ...'
        & $python.Exe @($python.Pre + @('-m', 'venv', '.venv'))
        if ($LASTEXITCODE -ne 0) { exit 1 }
    }

    $venvPython = Join-Path (Get-Location) '.venv\Scripts\python.exe'
    if (-not (Test-Path $venvPython)) {
        Write-Host 'error: python\.venv looks broken. Delete it and run this script again.' -ForegroundColor Red
        exit 1
    }

    Write-Host 'Installing dependencies ...'
    & $venvPython -m pip install --quiet --disable-pip-version-check -r requirements.txt
    if ($LASTEXITCODE -ne 0) { exit 1 }

    Write-Host 'Running the tests ...'
    $apiLine = Get-PytestSummary ((& $venvPython -m pytest tests\test_api.py) -join "`n")
    $integrationLine = Get-PytestSummary (
        (& $venvPython -m pytest tests\test_statement_builder.py tests\test_my_tests.py) -join "`n")
    $logHint = 'Re-run them any time with:  cd python; .\.venv\Scripts\python.exe -m pytest'
}
else {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Host 'error: java was not found on your PATH.' -ForegroundColor Red
        Write-Host '  install Temurin JDK 17 from https://adoptium.net/temurin/releases/?version=17'
        Write-Host '  during setup, tick "Set JAVA_HOME variable"'
        exit 1
    }
    if (-not $env:JAVA_HOME) {
        Write-Host 'warning: JAVA_HOME is not set. mvnw.cmd needs it.' -ForegroundColor Yellow
        Write-Host '  set it for this session with, for example:'
        Write-Host '  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17"'
    }
    Write-Host "Using $((java -version 2>&1 | Select-Object -First 1))"

    Set-Location (Join-Path $repoRoot 'java')
    Write-Host 'Resolving dependencies (the first run downloads Maven and can take 2-5 minutes) ...'
    & .\mvnw.cmd -B -q dependency:resolve
    if ($LASTEXITCODE -ne 0) { exit 1 }

    Write-Host 'Building and running the tests ...'
    if (-not (Test-Path 'target')) { New-Item -ItemType Directory 'target' | Out-Null }
    & .\mvnw.cmd -B test '-Dmaven.test.failure.ignore=true' > 'target\setup-tests.log'
    $apiLine = Get-SurefireSummary 'target\surefire-reports\com.cu.api.AccountsApiTest.txt'
    $integrationLine = Get-SurefireSummary 'target\surefire-reports\com.cu.integration.StatementBuilderTest.txt'
    $logHint = 'Full output is in java\target\setup-tests.log; re-run with:  cd java; .\mvnw.cmd test'
}

if (-not $apiLine) { $apiLine = 'could not read results' }
if (-not $integrationLine) { $integrationLine = 'could not read results' }

Write-Host ''
Write-Host '--------------------------------------------------------------------'
Write-Host ("API tests:          {0,-21} (expected: that failure is Part B)" -f $apiLine)
Write-Host ("Integration tests:  {0,-21} (expected: that failure is Part C)" -f $integrationLine)
Write-Host ''
Write-Host "Start the API with: powershell -ExecutionPolicy Bypass -File scripts\run.ps1 $Stack"
Write-Host $logHint
Write-Host '--------------------------------------------------------------------'
