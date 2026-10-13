param(
    [switch]$Once
)

$ErrorActionPreference = 'Stop'
$projectDirectory = $PSScriptRoot

$localJdk = Get-ChildItem -Path "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
    Select-Object -First 1
if ($null -ne $localJdk) {
    $env:JAVA_HOME = $localJdk.FullName
}

$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($null -eq $mavenCommand) {
    $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
}
if ($null -eq $mavenCommand) {
    $mavenPath = Get-ChildItem -Path "$env:ProgramFiles\JetBrains" -Directory -Filter 'IntelliJ IDEA*' -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        ForEach-Object { Join-Path $_.FullName 'plugins\maven\lib\maven3\bin\mvn.cmd' } |
        Where-Object { Test-Path $_ } |
        Select-Object -First 1
    if ($null -eq $mavenPath) {
        throw 'Maven est introuvable. Ajoutez mvn au PATH ou installez-le avec IntelliJ IDEA.'
    }
} else {
    $mavenPath = $mavenCommand.Source
}

if ($Once) {
    $env:AUTOFIX_RUN_ONCE = 'true'
}

Push-Location $projectDirectory
try {
    & $mavenPath -q compile exec:java '-Dexec.mainClass=org.infospray.peonsimulator.supervisor.adapter.primary.AutoCorrectionSupervisorMain'
    if ($LASTEXITCODE -ne 0) {
        throw "Le superviseur s'est terminé avec le code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
