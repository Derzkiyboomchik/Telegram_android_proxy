#Requires -Version 5.1
<#
.SYNOPSIS
    Сборка Go-разделяемых библиотек tg-ws-proxy для Android через Docker.
.DESCRIPTION
    Запускает build-go.sh в Git Bash. Требуется Docker Desktop.
#>

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$OutputDir = "$ProjectRoot/app/src/main/jniLibs"

Write-Host "=== TG WS Proxy Go Build ===" -ForegroundColor Cyan
Write-Host "Project root: $ProjectRoot"
Write-Host "Output dir:   $OutputDir"

# Проверяем Docker
$dockerInfo = docker info 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker Desktop не запущен или не установлен. Запустите Docker Desktop и повторите."
    exit 1
}

Write-Host "Running build-go.sh via Git Bash..." -ForegroundColor Yellow

$bashCmd = "bash `"$($ProjectRoot -replace '\\','/')/build-go.sh`""
Invoke-Expression $bashCmd

if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host "=== Build finished ===" -ForegroundColor Green
Get-ChildItem -Path $OutputDir -Recurse -File | ForEach-Object {
    Write-Host "  $($_.FullName)" -ForegroundColor Gray
}
