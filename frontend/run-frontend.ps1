$ErrorActionPreference = "Stop"

if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) {
    throw "Node.js 20 or newer is required. Install Node.js, then run this script again."
}

if (-not (Test-Path (Join-Path $PSScriptRoot "node_modules"))) {
    Write-Host "Installing frontend dependencies..." -ForegroundColor Yellow
    & npm.cmd install
}

Write-Host "Starting REELVAULT at http://localhost:3000" -ForegroundColor Yellow
& npm.cmd run dev
