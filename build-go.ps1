# TG WS Proxy Go cross-compilation script for Android ABIs (c-shared)
param(
    [string]$OutputDir = "$PSScriptRoot\app\src\main\jniLibs"
)

$ErrorActionPreference = "Stop"

$workspaceDir = ($PSScriptRoot -replace '\\', '/')
$outDir = ((Resolve-Path $OutputDir).Path -replace '\\', '/')

Write-Host "=== Building inside Docker (golang:1.23-bookworm) ==="
Write-Host "Workspace: $workspaceDir"
Write-Host "Output: $outDir"

docker run --rm `
    -v "ndk-cache:/ndk_cache" `
    -v "${workspaceDir}:/workspace:ro" `
    -v "${outDir}:/output" `
    golang:1.23-bookworm bash -c "tr -d '\r' < /workspace/build-internal.sh | bash"

Write-Host "=== Done ==="
