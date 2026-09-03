$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$backendJar = Join-Path $root 'backend\target\cloudread-backend-1.0.0.jar'
$frontendDir = Join-Path $root 'frontend'
$backendDir = Join-Path $root 'backend'

Write-Host '========================================' -ForegroundColor Cyan
Write-Host '  云阅 CloudRead 一键启动' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan

# 1. 检查后端 JAR，缺失时自动构建
if (-not (Test-Path $backendJar)) {
    Write-Host '==> 未找到后端 JAR，开始自动构建...' -ForegroundColor Yellow
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mvn) {
        $localMaven = 'C:\Users\111\.maven\apache-maven-3.9.9\bin\mvn.cmd'
        if (Test-Path $localMaven) { $mvn = $localMaven } else {
            Write-Host '未找到 Maven，请先运行 scripts\build-all.ps1 构建项目' -ForegroundColor Red
            exit 1
        }
    }
    Push-Location $backendDir
    try { & $mvn -q -DskipTests package } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $backendJar)) {
        Write-Host '后端构建失败，请检查 Maven 环境' -ForegroundColor Red
        exit 1
    }
}

# 2. 启动后端（未运行时）
try {
    $backendProc = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object { $_.CommandLine -like '*cloudread-backend*' }
} catch {
    $backendProc = $null
}
if ($backendProc) {
    Write-Host '==> 后端已在运行 (http://localhost:8080)' -ForegroundColor Green
} else {
    Write-Host '==> 启动后端...' -ForegroundColor Cyan
    $out = Join-Path $backendDir 'target\backend.log'
    $err = Join-Path $backendDir 'target\backend-err.log'
    Remove-Item -LiteralPath $out, $err -ErrorAction SilentlyContinue
    Start-Process -FilePath 'java' -ArgumentList '-jar', $backendJar `
        -WorkingDirectory $backendDir -WindowStyle Hidden `
        -RedirectStandardOutput $out -RedirectStandardError $err | Out-Null

    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        try {
            $null = Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/books?page=1&size=1' -UseBasicParsing -TimeoutSec 2
            $ready = $true
            break
        } catch {
            # 未就绪继续等待
        }
    }
    if ($ready) {
        Write-Host '==> 后端启动成功 (http://localhost:8080)' -ForegroundColor Green
    } else {
        Write-Host '==> 后端启动可能失败，请查看 backend\target\backend-err.log' -ForegroundColor Red
    }
}

# 3. 启动前端（未运行时）
try {
    $viteProc = Get-CimInstance Win32_Process -Filter "Name='node.exe'" |
        Where-Object { $_.CommandLine -like '*vite*5173*' }
} catch {
    $viteProc = $null
}
if ($viteProc) {
    Write-Host '==> 前端已在运行 (http://localhost:5173)' -ForegroundColor Green
} else {
    Write-Host '==> 启动前端...' -ForegroundColor Cyan
    $viteOut = Join-Path $frontendDir 'vite.log'
    $viteErr = Join-Path $frontendDir 'vite-err.log'
    Remove-Item -LiteralPath $viteOut, $viteErr -ErrorAction SilentlyContinue
    Start-Process -FilePath 'node' -ArgumentList 'node_modules\vite\bin\vite.js','--host','127.0.0.1','--port','5173' `
        -WorkingDirectory $frontendDir -WindowStyle Hidden `
        -RedirectStandardOutput $viteOut -RedirectStandardError $viteErr | Out-Null
    Start-Sleep -Seconds 3
    try {
        $null = Invoke-WebRequest -Uri 'http://127.0.0.1:5173/' -UseBasicParsing -TimeoutSec 3
        Write-Host '==> 前端启动成功 (http://localhost:5173)' -ForegroundColor Green
    } catch {
        Write-Host '==> 前端启动可能失败，请查看 frontend\vite-err.log' -ForegroundColor Red
    }
}

Write-Host ''
Write-Host '访问地址：' -ForegroundColor White
Write-Host '  前端  http://localhost:5173' -ForegroundColor Green
Write-Host '  后端  http://localhost:8080' -ForegroundColor Green
Write-Host '  接口文档 http://localhost:8080/doc.html' -ForegroundColor Green
Write-Host '停止服务请运行 stop.bat' -ForegroundColor Yellow
