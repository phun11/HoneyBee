/*
  HoneyBee Oracle - FULL RESET AND INIT wrapper.
  Chay bang SQL Developer: mo file nay trong connection HONEYBEE_WEB, bam F5 / Run Script.
  Chay bang terminal: sqlplus HONEYBEE_WEB/12345@localhost:1521/XEPDB1 @database/oracle/00_FULL_RESET_HONEYBEE_WEB.sql
*/
SET DEFINE OFF;
SET SERVEROUTPUT ON;
SET ECHO ON;
SET FEEDBACK ON;
SET TIMING ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

PROMPT ============================================================
PROMPT HoneyBee Oracle full reset started
SELECT USER FROM dual;
PROMPT ============================================================

@@01_drop_old_objects.sql
@@02_schema_oracle.sql
@@03_seed_demo_data.sql
@@04_optional_dbms_objects.sql
@@05_test_queries.sql
@@06_check_required_schema.sql

PROMPT ============================================================
PROMPT HoneyBee Oracle full reset completed successfully
PROMPT ============================================================
EXIT;
