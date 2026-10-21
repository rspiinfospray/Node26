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

$configuredCodexPath = $env:AUTOFIX_CODEX_COMMAND
if ($configuredCodexPath -and (Test-Path -LiteralPath $configuredCodexPath -PathType Leaf)) {
    $codexPath = (Resolve-Path -LiteralPath $configuredCodexPath).Path
} else {
    $codexCommand = Get-Command codex.cmd -ErrorAction SilentlyContinue
}
if ($null -eq $codexPath -and $null -eq $codexCommand) {
    $jetBrainsDirectory = Join-Path $env:LOCALAPPDATA 'JetBrains'
    $codexPath = Get-ChildItem -LiteralPath $jetBrainsDirectory -Directory -Filter 'IntelliJIdea*' -ErrorAction SilentlyContinue |
        ForEach-Object {
            $runtimeDirectory = Join-Path $_.FullName 'acp-agents\.runtimes\node'
            if (Test-Path -LiteralPath $runtimeDirectory) {
                Get-ChildItem -LiteralPath $runtimeDirectory -Filter 'codex.cmd' -File -Recurse -ErrorAction SilentlyContinue
            }
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    if ($null -eq $codexPath) {
        throw 'Codex CLI est introuvable. Installez Codex ou définissez AUTOFIX_CODEX_COMMAND avec le chemin complet de codex.cmd.'
    }
} elseif ($null -eq $codexPath) {
    $codexPath = $codexCommand.Source
}
$codexAncestor = (Get-Item -LiteralPath $codexPath).Directory
while ($null -ne $codexAncestor) {
    $nodeDirectory = Join-Path $codexAncestor.FullName 'bin'
    if (Test-Path -LiteralPath (Join-Path $nodeDirectory 'node.exe') -PathType Leaf) {
        $env:Path = $nodeDirectory + [IO.Path]::PathSeparator + $env:Path
        break
    }
    $codexAncestor = $codexAncestor.Parent
}
$env:AUTOFIX_CODEX_COMMAND = $codexPath

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
