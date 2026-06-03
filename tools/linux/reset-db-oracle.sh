#!/usr/bin/env bash
set -e
DB_USER=${DB_USER:-HONEYBEE_WEB}
DB_PASS=${DB_PASS:-12345}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-1521}
DB_SERVICE=${DB_SERVICE:-XEPDB1}
cd "$(dirname "$0")/../.."
echo "[HoneyBee] Reset Oracle schema ${DB_USER} on ${DB_HOST}:${DB_PORT}/${DB_SERVICE}"
sqlplus "${DB_USER}/${DB_PASS}@${DB_HOST}:${DB_PORT}/${DB_SERVICE}" @database/oracle/00_RUN_ALL_RESET_AND_SEED.sql
