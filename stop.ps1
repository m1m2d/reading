$ErrorActionPreference = 'Continue'

Write-Host '========================================' -ForegroundColor Cyan
Write-Host '  云阅 CloudRead 一键停止' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan

function Find-Proc {
    param([string]$Name, [string]$Pattern)
    try {
        return Get-CimInstance Win32_Process -Filter "Name='$Name'" |
            Where-Object { $_.CommandLine -like $Pattern }
    } catch {
        Write-Host "枚举 $Name 进程失败（可能需要管理员权限）：$($_.Exception.Message)" -ForegroundColor Red
        Write-Host '可改为手动停止：任务管理器结束 java.exe / node.exe' -ForegroundColor Yellow
        exit 1
    }
}

# 停止后端
$backend = Find-Proc -Name 'java.exe' -Pattern '*cloudread-backend*'
if ($backend) {
    $backend | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
    Write-Host '==> 后端已停止' -ForegroundColor Green
} else {
    Write-Host '==> 后端未在运行' -ForegroundColor Yellow
}

# 停止前端 Vite
$vite = Find-Proc -Name 'node.exe' -Pattern '*vite*5173*'
if ($vite) {
    $vite | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
    Write-Host '==> 前端已停止' -ForegroundColor Green
} else {
    Write-Host '==> 前端未在运行' -ForegroundColor Yellow
}

Write-Host '全部服务已停止' -ForegroundColor White
