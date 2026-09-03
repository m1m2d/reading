@echo off
chcp 65001 >nul
title 云阅 CloudRead 一键启动
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"
echo.
pause
