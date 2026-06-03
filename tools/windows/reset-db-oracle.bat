@echo off
chcp 65001 >nul
set NLS_LANG=AMERICAN_AMERICA.AL32UTF8
setlocal
REM ============================================================
REM HoneyBee Web Oracle - reset database from terminal
REM Run from project root: tools\windows\reset-db-oracle.bat
REM ============================================================
set DB_USER=HONEYBEE_WEB
set DB_PASS=12345
set DB_HOST=localhost
set DB_PORT=1521
set DB_SERVICE=XEPDB1

cd /d "%~dp0\..\.."

echo [HoneyBee] Reset Oracle schema %DB_USER% on %DB_HOST%:%DB_PORT%/%DB_SERVICE%
echo [HoneyBee] If sqlplus is not recognized, add Oracle XE bin folder to PATH.

echo.
sqlplus %DB_USER%/%DB_PASS%@%DB_HOST%:%DB_PORT%/%DB_SERVICE% @database\oracle\00_RUN_ALL_RESET_AND_SEED.sql
if errorlevel 1 (
  echo.
  echo [HoneyBee] DB reset FAILED. Check the SQL error above.
  pause
  exit /b 1
)

echo.
echo [HoneyBee] DB reset completed.
pause
