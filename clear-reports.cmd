@echo off
setlocal

set "NODE26_REPORTS_DIRECTORY=%~dp0reports"

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$reportsDirectory = [System.IO.Path]::GetFullPath($env:NODE26_REPORTS_DIRECTORY);" ^
  "if ([System.IO.Path]::GetFileName($reportsDirectory.TrimEnd([char[]]'\/')) -ne 'reports') { throw 'Le repertoire cible n''est pas le repertoire reports attendu.' };" ^
  "[System.IO.Directory]::CreateDirectory($reportsDirectory) | Out-Null;" ^
  "$files = @(Get-ChildItem -LiteralPath $reportsDirectory -File -Recurse -Force);" ^
  "$files | Remove-Item -Force;" ^
  "[System.IO.Directory]::CreateDirectory((Join-Path $reportsDirectory 'autofix-state')) | Out-Null;" ^
  "[System.IO.Directory]::CreateDirectory((Join-Path $reportsDirectory 'codex-results')) | Out-Null;" ^
  "Write-Host ($files.Count.ToString() + ' fichier(s) supprime(s). Structure du superviseur conservee.')"

if errorlevel 1 (
    echo Echec du nettoyage des rapports.
    exit /b 1
)

exit /b 0
