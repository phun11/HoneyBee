# HoneyBee Oracle Web - chạy bằng Terminal VS Code

Bản này dùng Oracle, không dùng H2. Vì vậy trong giao diện Admin không còn H2 Console. Database chính là schema `HONEYBEE_WEB` trong Oracle.

## 1. Yêu cầu môi trường

Cài sẵn:

- JDK 17 trở lên.
- Oracle XE đang chạy.
- Oracle SQL Developer để kiểm tra DB khi cần.
- VS Code.
- Có schema Oracle `HONEYBEE_WEB / 12345`.

Connection mặc định của project:

```text
Username: HONEYBEE_WEB
Password: 12345
Host: localhost
Port: 1521
Service name: XEPDB1
```

Nếu máy bạn dùng `FREEPDB1`, sửa trong các file sau:

```text
tools/windows/reset-db-oracle.bat
tools/windows/check-db-oracle.bat
backend/src/main/resources/application.properties
```

Đổi `XEPDB1` thành `FREEPDB1`.

## 2. Reset sạch DB và nạp lại toàn bộ bảng/data/trigger/procedure

Mở VS Code tại thư mục gốc project, mở Terminal rồi chạy:

```powershell
.\tools\windows\reset-db-oracle.bat
```

Nếu PowerShell không chạy `.bat`, dùng:

```powershell
cmd /c tools\windows\reset-db-oracle.bat
```

Script này sẽ chạy:

```text
database/oracle/00_RUN_ALL_RESET_AND_SEED.sql
```

Nó tự thực hiện:

```text
1. Xóa object HoneyBee cũ trong schema HONEYBEE_WEB
2. Tạo lại bảng Oracle
3. Nạp seed data ban đầu
4. Tạo function/procedure/trigger/cursor demo
5. Kiểm tra bảng/cột bắt buộc
```

Lưu ý: script chỉ nên chạy trong schema test `HONEYBEE_WEB`, không chạy trong schema đồ án khác.

## 3. Nếu terminal báo `sqlplus is not recognized`

Cách 1: thêm Oracle bin vào PATH, ví dụ:

```text
C:\app\<ten-may>\product\21c\dbhomeXE\bin
```

Cách 2: dùng SQL Developer:

- Mở connection `HONEYBEE_WEB`.
- Mở file:

```text
database/oracle/00_RUN_ALL_RESET_AND_SEED.sql
```

- Bấm `F5 / Run Script`.
- Không dùng `Ctrl + Enter`.

## 4. Kiểm tra DB sau khi reset

Chạy trong VS Code Terminal:

```powershell
.\tools\windows\check-db-oracle.bat
```

Hoặc trong SQL Developer chạy:

```sql
SELECT column_name
FROM user_tab_columns
WHERE table_name = 'PRODUCTS'
ORDER BY column_id;
```

Bảng `PRODUCTS` phải có các cột mới:

```text
STORE_ID
TRANSPORTER_ID
PICKUP_LOCATION
DELIVERY_LOCATION
PRODUCT_IMAGE_B64
PRODUCT_IMAGE_MIME
REQUIRED_TEMP_MIN
REQUIRED_TEMP_MAX
REQUIRED_HUMIDITY_MIN
REQUIRED_HUMIDITY_MAX
```

## 5. Chạy web backend

Trong VS Code Terminal:

```powershell
.\tools\windows\run-web-oracle.bat
```

Hoặc chạy tay:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Chờ thấy dòng tương tự:

```text
Tomcat started on port 8080
Started AgriTraceApplication
```

Không tắt terminal này khi đang test web.

## 6. Kiểm tra API health

Mở trình duyệt:

```text
http://localhost:8080/api/system/health
```

Kết quả đúng phải có:

```json
"ready": true
```

Nếu `ready:false`, kiểm tra `missingTables` và `missingColumns`, sau đó reset DB lại.

## 7. Mở web

```text
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

## 8. Test ảnh sản phẩm lưu DB

Luồng test:

```text
1. Đăng nhập farm1 / 123456
2. Mở http://localhost:8080/farm-management.html
3. Chọn ảnh trong mục "Ảnh sản phẩm do Farm upload"
4. Nhập đủ thông tin lô
5. Bấm "+ Tạo lô và sinh QR"
6. Bấm "Xác nhận chờ lấy hàng" nếu muốn chuyển sang Transport
7. Mở products/transport/store/trace để kiểm tra ảnh hiển thị
```

Ảnh được frontend đọc thành Base64 và gửi vào backend qua JSON. Backend lưu vào Oracle:

```text
PRODUCTS.PRODUCT_IMAGE_B64
PRODUCTS.PRODUCT_IMAGE_MIME
```

Để tránh request quá lớn, ảnh upload nên nhỏ hơn 900KB.

## 9. Lỗi chữ tiếng Việt bị vỡ

Seed data trong `03_seed_demo_data.sql` đã chuyển sang ASCII không dấu để tránh lỗi SQLPlus trên Windows ghi sai encoding. Các trang HTML vẫn dùng UTF-8.

Nếu bạn muốn seed tiếng Việt có dấu, cần đảm bảo terminal dùng UTF-8:

```bat
chcp 65001
set NLS_LANG=AMERICAN_AMERICA.AL32UTF8
```

Bản `.bat` hiện đã set sẵn 2 dòng này.

## 10. Thứ tự chạy chuẩn mỗi lần test

Lần đầu hoặc khi DB lỗi:

```powershell
.\tools\windows\reset-db-oracle.bat
.\tools\windows\run-web-oracle.bat
```

Những lần sau, nếu không muốn xóa dữ liệu test:

```powershell
.\tools\windows\run-web-oracle.bat
```


## Cap nhat seed/test data va Excel import

Ban nay nap san 10 lo nong san de test Farm - Transport - Store - Trace:

| Token | Muc dich test |
|---|---|
| HB-QR-VEG-001-SAFE | SAFE: 10 luot quet trong ngay, chua ban, con han |
| HB-QR-MANGO-002-WARN40 | WARNING: seed san 41 luot quet trong ngay |
| HB-QR-CABBAGE-006-TEMP | DANGER: co canh bao nhiet do van chuyen vuot nguong |
| HB-QR-DRAGON-008-SOLD | DANGER: QR da ban/SOLD nhung van quet lai |
| HB-QR-SPINACH-009-EXPIRED | DANGER: lo/giay chung nhan het han |
| HB-QR-BASIL-010-BROKENSEAL | DANGER: seal rach, store tu choi |

Quy tac canh bao QR hien tai:

- 0 den 40 luot quet/ngay: SAFE neu khong co rui ro khac.
- Tu luot thu 41 tro di: WARNING mau vang.
- DANGER mau do khi co dieu kien nguy hiem kem theo: QR da ban, lo ban het/het han, QR revoked/expired, sai chu ky, nhiet do/niem phong/su co nghiem trong.

File Excel test mau duoc dat o thu muc goc project sau khi giai nen:

```text
honeybee_farm_import_10_products.xlsx
```

Luồng import Excel:

1. Dang nhap `farm1 / 123456`.
2. Mo `http://localhost:8080/farm-management.html`.
3. Chon file `honeybee_farm_import_10_products.xlsx`.
4. He thong hien khung xem truoc: tong so dong, hop le, loi, danh sach loi tung dong.
5. Bam `Xac nhan tao cac lo hop le`.
6. Sau khi import, UI hien so dong thanh cong, so dong that bai va danh sach `PRODUCT_ID` / ma lo vua tao.

Chay lai toan bo DB va web:

```powershell
.\tools\windows\reset-db-oracle.bat
.\tools\windows\run-web-oracle.bat
```

Kiem tra health:

```text
http://localhost:8080/api/system/health
```

Phai thay `ready: true` truoc khi test UI.

## Cập nhật V4 - import ảnh từ Excel và giao diện danh sách lô

Farm import Excel hiện hỗ trợ thêm các cột ảnh:

- `imageUrl`: đường dẫn ảnh, ví dụ `assets/veg.svg`, `assets/mango.svg`, `assets/tomato.svg` hoặc URL ảnh HTTP/HTTPS.
- `productImageBase64`: nội dung ảnh dạng base64. Có thể dùng cả dạng đầy đủ `data:image/png;base64,...`.
- `productImageMime`: kiểu ảnh, ví dụ `image/png`, `image/jpeg`, `image/svg+xml`.

Khi Farm bấm **Xác nhận tạo các lô hợp lệ**, nếu dòng Excel có `imageUrl` và trình duyệt đọc được ảnh, hệ thống sẽ tự chuyển ảnh đó thành base64 và lưu vào Oracle DB ở `PRODUCTS.PRODUCT_IMAGE_B64` / `PRODUCTS.PRODUCT_IMAGE_MIME`. Nếu không đọc được ảnh, hệ thống vẫn lưu `IMAGE_URL` để hiển thị dự phòng.

File mẫu mới nằm ở thư mục gốc project:

```text
honeybee_farm_import_10_products.xlsx
```

Sau khi cập nhật bản này, nên chạy lại toàn bộ:

```powershell
.\tools\windows\reset-db-oracle.bat
.\tools\windows\run-web-oracle.bat
```

Sau đó mở:

```text
http://localhost:8080/api/system/health
```

Khi `ready: true`, test bằng:

```text
farm1 / 123456
transport1 / 123456
store1 / 123456
```
