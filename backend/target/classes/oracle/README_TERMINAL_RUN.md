# HoneyBee Web Oracle - chạy DB bằng terminal

## Mục tiêu

Các file trong thư mục `database/oracle` dùng để xóa sạch schema test `HONEYBEE_WEB`, tạo lại toàn bộ bảng Oracle, nạp seed data, tạo function/procedure/trigger và kiểm tra dữ liệu. Dùng bộ script này để tránh tình trạng backend đã là bản mới nhưng Oracle vẫn còn bảng cũ như lỗi `ORA-00904: "P"."STORE_ID": invalid identifier`.

## File chính

- `00_RUN_ALL_RESET_AND_SEED.sql`: chạy một lần để reset + tạo schema + seed + PL/SQL + test.
- `01_drop_old_objects.sql`: xóa bảng/view/procedure/function cũ trong schema hiện tại.
- `02_schema_oracle.sql`: tạo bảng, khóa chính, khóa ngoại, constraint, index, view `VW_PUBLIC_TRACE`.
- `03_seed_demo_data.sql`: dữ liệu ban đầu gồm 3 farm, 2 transporter, 2 store, user demo, lô hàng, QR, chứng chỉ, transport history.
- `04_optional_dbms_objects.sql`: function/procedure/trigger để nộp phần DBMS.
- `05_test_queries.sql`: kiểm tra nhanh sau khi chạy.
- `06_check_required_schema.sql`: kiểm tra bảng/cột bắt buộc.

## Chạy trên Windows bằng batch file

Mở CMD hoặc PowerShell tại thư mục gốc project:

```bat
tools\windows\reset-db-oracle.bat
```

Sau đó chạy web:

```bat
tools\windows\run-web-oracle.bat
```

Kiểm tra API:

```bat
tools\windows\check-api-health.bat
```

## Chạy trực tiếp bằng sqlplus

Từ thư mục gốc project:

```bat
sqlplus HONEYBEE_WEB/12345@localhost:1521/XEPDB1 @database\oracle\00_RUN_ALL_RESET_AND_SEED.sql
```

Nếu máy dùng `FREEPDB1`, sửa service name:

```bat
sqlplus HONEYBEE_WEB/12345@localhost:1521/FREEPDB1 @database\oracle\00_RUN_ALL_RESET_AND_SEED.sql
```

## Chạy bằng SQL Developer

1. Mở connection `HONEYBEE_WEB`.
2. File → Open → chọn `database/oracle/00_RUN_ALL_RESET_AND_SEED.sql`.
3. Bấm **F5 / Run Script**.
4. Không bấm `Ctrl + Enter` vì `Ctrl + Enter` chỉ chạy một câu đang chọn.

## Kiểm tra sau khi reset

Trong SQL Developer hoặc terminal:

```sql
SELECT column_name
FROM user_tab_columns
WHERE table_name = 'PRODUCTS'
ORDER BY column_id;
```

Bảng `PRODUCTS` phải có các cột nghiệp vụ mới:

```text
STORE_ID
TRANSPORTER_ID
PICKUP_LOCATION
DELIVERY_LOCATION
QUANTITY
UNIT
REQUIRED_TEMP_MIN
REQUIRED_TEMP_MAX
REQUIRED_HUMIDITY_MIN
REQUIRED_HUMIDITY_MAX
TRANSPORT_NOTE
RECEIVER_NAME
RECEIVER_PHONE
EXPECTED_DELIVERY_AT
```

## Link test web

Sau khi backend chạy:

```text
http://localhost:8080/api/system/health
http://localhost:8080/login.html
http://localhost:8080/admin.html
http://localhost:8080/farm-management.html
http://localhost:8080/transport.html
http://localhost:8080/store.html
http://localhost:8080/products.html
http://localhost:8080/scanner.html?refresh=1
```

Tài khoản demo:

```text
admin / admin123
farm1 / 123456
farm2 / 123456
farm3 / 123456
transport1 / 123456
transport2 / 123456
store1 / 123456
store2 / 123456
```
