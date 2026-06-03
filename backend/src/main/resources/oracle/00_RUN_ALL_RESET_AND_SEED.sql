/*
  HoneyBee Web Oracle - chạy 1 file để reset sạch schema và nạp lại dữ liệu demo.
  Cách chạy terminal:
    sqlplus HONEYBEE_WEB/12345@localhost:1521/XEPDB1 @database/oracle/00_RUN_ALL_RESET_AND_SEED.sql
  Cách chạy SQL Developer:
    Mở connection HONEYBEE_WEB -> File Open file này -> nhấn F5 / Run Script.
*/
SET DEFINE OFF;
SET SERVEROUTPUT ON;
SET ECHO ON;
SET FEEDBACK ON;
SET TIMING ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

PROMPT ============================================================
PROMPT HoneyBee Oracle reset started
PROMPT Current schema:
SELECT USER FROM dual;
PROMPT ============================================================

PROMPT [1/5] Drop old HoneyBee Web objects
@@01_drop_old_objects.sql

PROMPT [2/5] Create Oracle schema / tables / indexes / view
@@02_schema_oracle.sql

PROMPT [3/5] Insert initial seed data
@@03_seed_demo_data.sql

PROMPT [4/5] Create function / procedure / trigger / cursor demo
@@04_optional_dbms_objects.sql

PROMPT [5/5] Verify data and required columns
@@05_test_queries.sql

PROMPT Extra check: PRODUCTS must have STORE_ID / TRANSPORTER_ID / logistics columns
SELECT column_name
FROM user_tab_columns
WHERE table_name = 'PRODUCTS'
  AND column_name IN (
    'STORE_ID','TRANSPORTER_ID','PRODUCT_IMAGE_B64','PRODUCT_IMAGE_MIME','PICKUP_LOCATION','DELIVERY_LOCATION',
    'QUANTITY','UNIT','REQUIRED_TEMP_MIN','REQUIRED_TEMP_MAX',
    'REQUIRED_HUMIDITY_MIN','REQUIRED_HUMIDITY_MAX','TRANSPORT_NOTE',
    'RECEIVER_NAME','RECEIVER_PHONE','EXPECTED_DELIVERY_AT'
  )
ORDER BY column_id;

PROMPT ============================================================
PROMPT HoneyBee Oracle reset completed successfully
PROMPT ============================================================
EXIT;
