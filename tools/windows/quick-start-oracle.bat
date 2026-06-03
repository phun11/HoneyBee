@echo off
setlocal
cd /d "%~dp0\..\.."
echo ============================================================
echo HoneyBee Oracle quick start
echo 1. Reset Oracle DB
echo 2. Start backend
echo ============================================================
call tools\windows\reset-db-oracle.bat
if errorlevel 1 exit /b 1
call tools\windows\run-web-oracle.bat
