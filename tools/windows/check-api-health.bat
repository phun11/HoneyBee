@echo off
setlocal
echo [HoneyBee] Checking backend health...
curl http://localhost:8080/api/system/health
echo.
echo [HoneyBee] Checking dashboard...
curl http://localhost:8080/api/system/dashboard
echo.
pause
