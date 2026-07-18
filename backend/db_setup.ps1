[CmdletBinding()]
param(
    [string]$PostgresHost = $env:PGHOST,
    [ValidateRange(0, 65535)]
    [int]$PostgresPort = 0,
    [string]$AdminUser = $env:PGUSER
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($PostgresHost)) {
    $PostgresHost = 'localhost'
}
if ($PostgresPort -eq 0) {
    $PostgresPort = if ($env:PGPORT) { [int]$env:PGPORT } else { 5432 }
}
if ([string]::IsNullOrWhiteSpace($AdminUser)) {
    $AdminUser = 'postgres'
}

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw 'psql was not found. Install the PostgreSQL command-line tools and add them to PATH.'
}

$mavenWrapper = Join-Path $PSScriptRoot 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $mavenWrapper)) {
    throw "Maven wrapper not found at $mavenWrapper."
}

$temporaryPassword = [string]::IsNullOrWhiteSpace($env:PGPASSWORD)
if ($temporaryPassword) {
    $securePassword = Read-Host "Password for PostgreSQL administrator '$AdminUser'" -AsSecureString
    $credential = New-Object System.Management.Automation.PSCredential($AdminUser, $securePassword)
    $env:PGPASSWORD = $credential.GetNetworkCredential().Password
}

$bootstrapSql = @'
\set ON_ERROR_STOP on
SELECT pg_advisory_lock(hashtext('livestream-db-setup'));

DO $setup$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'livestream') THEN
        CREATE ROLE livestream LOGIN PASSWORD 'livestream';
    ELSE
        ALTER ROLE livestream WITH LOGIN PASSWORD 'livestream';
    END IF;
END
$setup$;

SELECT 'CREATE DATABASE livestream OWNER livestream'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'livestream')
\gexec

ALTER DATABASE livestream OWNER TO livestream;
\connect livestream
GRANT USAGE, CREATE ON SCHEMA public TO livestream;
'@

Push-Location $PSScriptRoot
try {
    Write-Host "Preparing PostgreSQL at ${PostgresHost}:$PostgresPort..."
    $bootstrapSql | & psql -X --host=$PostgresHost --port=$PostgresPort --username=$AdminUser --dbname=postgres
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL bootstrap failed with exit code $LASTEXITCODE."
    }

    Write-Host 'Applying Flyway migrations...'
    & $mavenWrapper flyway:migrate `
        "-Dflyway.url=jdbc:postgresql://${PostgresHost}:${PostgresPort}/livestream" `
        '-Dflyway.user=livestream' `
        '-Dflyway.password=livestream'
    if ($LASTEXITCODE -ne 0) {
        throw "Flyway migration failed with exit code $LASTEXITCODE."
    }

    Write-Host 'Database setup complete.' -ForegroundColor Green
}
finally {
    Pop-Location
    if ($temporaryPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
}
