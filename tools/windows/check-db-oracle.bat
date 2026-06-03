@echo off
chcp 65001 >nul
set NLS_LANG=AMERICAN_AMERICA.AL32UTF8
setlocal
set DB_USER=HONEYBEE_WEB
set DB_PASS=12345
set DB_HOST=localhost
set DB_PORT=1521
set DB_SERVICE=XEPDB1
cd /d "%~dp0\..\.."
sqlplus %DB_USER%/%DB_PASS%@%DB_HOST%:%DB_PORT%/%DB_SERVICE% @database\oracle\06_check_required_schema.sql
pause
