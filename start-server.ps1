$ErrorActionPreference = 'Stop'

$wrapperPath = Join-Path $PSScriptRoot 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $wrapperPath)) {
    throw 'Place this script next to pom.xml and mvnw.cmd.'
}

$previousDbPassword = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'Process')
$dbSecret = $null

Push-Location -LiteralPath $PSScriptRoot
try {
    $dbSecret = Read-Host 'Enter PostgreSQL password' -AsSecureString
    if ($dbSecret.Length -eq 0) {
        throw 'The PostgreSQL password cannot be empty.'
    }

    $credential = [System.Management.Automation.PSCredential]::new('postgres', $dbSecret)
    $env:DB_PASSWORD = $credential.GetNetworkCredential().Password

    Write-Host 'Starting the server. Press Ctrl+C to stop.'
    & $wrapperPath 'spring-boot:run'
}
finally {
    [Environment]::SetEnvironmentVariable('DB_PASSWORD', $previousDbPassword, 'Process')
    if ($null -ne $dbSecret) {
        $dbSecret.Dispose()
    }
    $credential = $null
    $previousDbPassword = $null
    Pop-Location
}
