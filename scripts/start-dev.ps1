$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$backendJar = Join-Path $root 'backend\target\cloudread-backend-1.0.0.jar'

if (-not (Test-Path $backendJar)) {
    Write-Host '后端 JAR 不存在，请先运行 build-all.ps1' -ForegroundColor Yellow
    exit 1
}

$backendProc = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like '*cloudread-backend*' }
if ($backendProc) {
    Write-Host '后端已在运行' -ForegroundColor Yellow
} else {
    Write-Host '==> 启动后端 (http://localhost:8080)' -ForegroundColor Cyan
    Start-Process -FilePath 'java' -ArgumentList '-jar', $backendJar `
        -WorkingDirectory (Join-Path $root 'backend') -WindowStyle Hidden
}

Write-Host '==> 启动前端 (http://localhost:5173)' -ForegroundColor Cyan
Push-Location (Join-Path $root 'frontend')
try {
    npm run dev
} finally {
    Pop-Location
}
