$ErrorActionPreference = "Stop"

# Load only known local settings. Values are never echoed to the terminal.
$envFile = Join-Path $PSScriptRoot ".env"
$allowedEnv = @("TMDB_READ_ACCESS_TOKEN", "REELVAULT_CORS_ORIGINS", "REELVAULT_COOKIE_SECURE", "REELVAULT_DATABASE_URL", "REELVAULT_DATABASE_DRIVER", "REELVAULT_HIBERNATE_DIALECT")
if (Test-Path $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile) {
        if ($line -match '^\s*([^#=\s]+)\s*=\s*(.*)\s*$' -and $allowedEnv -contains $Matches[1]) {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], "Process")
        }
    }
}

# Desktop processes can predate a newly saved Windows user environment value.
# Refresh the TMDB token for this backend process without echoing it.
if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable("TMDB_READ_ACCESS_TOKEN", "Process"))) {
    $savedTmdbToken = [Environment]::GetEnvironmentVariable("TMDB_READ_ACCESS_TOKEN", "User")
    if (-not [string]::IsNullOrWhiteSpace($savedTmdbToken)) {
        [Environment]::SetEnvironmentVariable("TMDB_READ_ACCESS_TOKEN", $savedTmdbToken, "Process")
    }
}

$jdkCandidates = @(
    $env:JAVA_HOME,
    "C:\Program Files\Java\jdk-17",
    "C:\Program Files\Java\jdk-21"
) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) }

if (-not $jdkCandidates) {
    throw "Java 17 or newer is required. Install a JDK, then run this script again."
}

$env:JAVA_HOME = $jdkCandidates[0]
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$repo = Join-Path $PSScriptRoot ".tools\m2"
$wrapper = Join-Path $PSScriptRoot "mvnw.cmd"
$env:MAVEN_USER_HOME = Join-Path $PSScriptRoot ".tools\maven-user-home"

Write-Host "Starting REELVAULT API at http://localhost:8081" -ForegroundColor Yellow
if (Test-Path $wrapper) {
    & $wrapper "-Dmaven.repo.local=$repo" spring-boot:run
} else {
    $localMaven = Join-Path $PSScriptRoot ".tools\apache-maven-3.10.0\bin\mvn.cmd"
    $maven = if (Test-Path $localMaven) { $localMaven } else { (Get-Command mvn.cmd -ErrorAction Stop).Source }
    & $maven "-Dmaven.repo.local=$repo" spring-boot:run
}
