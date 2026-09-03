$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    $localMaven = 'C:\Users\111\.maven\apache-maven-3.9.9\bin\mvn.cmd'
    if (Test-Path $localMaven) { $mvn = $localMaven } else { throw '未找到 Maven，请安装并加入 PATH' }
}

Write-Host '==> 构建后端' -ForegroundColor Cyan
Push-Location (Join-Path $root 'backend')
try {
    & $mvn -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw '后端构建失败' }
} finally {
    Pop-Location
}

Write-Host '==> 安装并构建前端' -ForegroundColor Cyan
Push-Location (Join-Path $root 'frontend')
try {
    if (-not (Test-Path 'node_modules')) { npm install }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw '前端构建失败' }
} finally {
    Pop-Location
}

Write-Host '构建完成：backend/target/cloudread-backend-1.0.0.jar 与 frontend/dist/' -ForegroundColor Green
