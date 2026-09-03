@echo off
chcp 65001 >nul
title 云阅 CloudRead 一键停止
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1"
echo.
pause
