@echo off
setlocal
cd /d "%~dp0\..\..\backend"
echo [HoneyBee] Starting Spring Boot backend on http://localhost:8080
call mvnw.cmd spring-boot:run
pause
