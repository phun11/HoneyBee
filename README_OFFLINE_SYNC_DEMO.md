# HoneyBee Offline Sync Demo

## Lưu ý quan trọng

Nếu bạn tắt Spring Boot rồi bấm reload ngay và thấy Chrome báo `localhost refused to connect`, nghĩa là trang chưa được service worker cache hoặc chưa được service worker điều khiển.

Cách đúng để demo:

1. Chạy backend.
2. Mở `http://localhost:8080/transport.html`.
3. Chờ thông báo: `Offline cache đã sẵn sàng`.
4. Không đóng tab đó.
5. Tắt backend bằng `Ctrl + C`.
6. Thao tác trên trang Transport đang mở.
7. Web sẽ lưu thao tác vào IndexedDB.
8. Bật backend lại.
9. Web tự sync lên Oracle và ghi audit log.

## Chạy project

Mở terminal tại thư mục gốc project:

```powershell
cmd /c tools\windows\reset-db-oracle.bat
cmd /c tools\windows\run-web-oracle.bat
```

Mở:

```text
http://localhost:8080/login.html
```

Đăng nhập:

```text
transport1 / 123456
```

Vào:

```text
http://localhost:8080/transport.html
```

## Demo mất kết nối backend

### Bước 1: Mở sẵn trang Transport

Chờ trang load xong lô hàng và hiện thông báo offline cache sẵn sàng.

### Bước 2: Tắt backend

Ở terminal đang chạy Spring Boot:

```text
Ctrl + C
Y
```

### Bước 3: Thao tác offline

Trên trang Transport đang mở, chọn một lô, nhập chặng mới:

```text
Vị trí hiện tại: Tram trung chuyen Bao Loc
Nhiệt độ: 7.5
Độ ẩm: 82
Niêm phong: Nguyên vẹn
Ghi chú: Cap nhat khi mat ket noi backend
```

Bấm lưu chặng.

Kết quả mong muốn:

```text
Mất kết nối backend/Oracle. Thao tác đã được lưu tạm trên thiết bị và sẽ tự đồng bộ khi reconnect.
```

### Bước 4: Kiểm tra local queue

Mở DevTools:

```text
F12 → Application → IndexedDB
```

Tìm database HoneyBee offline queue và xem action `TRANSPORT_UPDATE_STEP` đang pending.

### Bước 5: Bật backend lại

Mở terminal:

```powershell
cmd /c tools\windows\run-web-oracle.bat
```

Khi backend chạy lại, trang sẽ hiện:

```text
Đang đồng bộ ... thao tác offline lên Oracle...
Đồng bộ thành công ... Audit log đã được ghi vào Oracle.
```

## Kiểm tra Oracle

```sql
SELECT *
FROM TRANSPORT_HISTORY
ORDER BY TRANSPORT_ID DESC;
```

```sql
SELECT *
FROM OFFLINE_SYNC_LOGS
ORDER BY SYNC_ID DESC;
```

```sql
SELECT AUDIT_ID, TABLE_NAME, RECORD_ID, ACTION_TYPE, SOURCE_TYPE,
       CLIENT_ACTION_ID, DEVICE_ID, OFFLINE_CREATED_AT, SYNCED_AT, CREATED_AT
FROM AUDIT_LOGS
ORDER BY AUDIT_ID DESC;
```

Dòng sync đúng sẽ có:

```text
SOURCE_TYPE = OFFLINE_SYNC
```

## Demo ngắt Oracle DB thật

Cách này cần mở PowerShell bằng quyền Administrator.

Tắt Oracle:

```powershell
net stop OracleServiceXE
```

Bật lại Oracle:

```powershell
net start OracleServiceXE
```

Nếu service khác tên:

```powershell
sc query state= all | findstr /I Oracle
```

Khuyến nghị demo trước lớp bằng cách tắt backend, vì dễ kiểm soát và ổn định hơn.

---

## Offline QR Trace Cache - quét QR vẫn hiện thông tin khi mất Wi-Fi/backend

Bản này bổ sung thêm cơ chế cache dữ liệu truy xuất QR để khách hàng vẫn xem được thông tin sản phẩm khi mất kết nối.

### Cơ chế hoạt động

Khi web còn online, các trang có `js/pwa.js` như Admin, Farm, Store, Products, Scanner và Product Detail sẽ tự gọi API:

```text
GET /api/system/offline-trace-cache
```

API này trả về dữ liệu trace đầy đủ cho các QR hiện có nhưng **không ghi QR_SCAN_LOGS**, vì đây chỉ là bước tải cache nội bộ. Dữ liệu được lưu vào IndexedDB store:

```text
honeybee-offline-db / traceCache
```

Khi mất kết nối backend hoặc mất Wi-Fi:

```text
Quét QR / nhập token
→ mở product-detail.html từ Service Worker cache
→ gọi API thất bại
→ đọc dữ liệu trong IndexedDB traceCache
→ hiển thị thông tin sản phẩm với nhãn OFFLINE TRACE
```

### Cách demo QR offline

1. Chạy backend bình thường:

```powershell
cmd /c tools\windows\run-web-oracle.bat
```

2. Mở một trong các trang sau để service worker và trace cache được cài:

```text
http://localhost:8080/admin.html
http://localhost:8080/farm-management.html
http://localhost:8080/store.html
http://localhost:8080/scanner.html
```

3. Chờ thông báo:

```text
Offline cache đã sẵn sàng
Đã cache ... hồ sơ QR để quét offline
```

4. Có thể kiểm tra cache bằng Chrome DevTools:

```text
F12 → Application → IndexedDB → honeybee-offline-db → traceCache
```

5. Tắt backend bằng `Ctrl + C` trong terminal.

6. Không cần server nữa, mở lại hoặc giữ trang:

```text
http://localhost:8080/scanner.html
```

Nếu scanner page đã được cache trước đó, Service Worker sẽ trả trang từ cache.

7. Quét QR hoặc nhập token đã cache, ví dụ:

```text
HB-QR-CAIXANH-001-SAFE
HB-QR-XOAI-002-WARN40
HB-QR-THANHLONG-008-SOLD
```

8. Trang `product-detail.html` sẽ hiện thông tin sản phẩm từ local cache và có nhãn:

```text
OFFLINE TRACE - Đang hiển thị dữ liệu sản phẩm đã lưu local
```

### Lưu ý khi demo

- Phải mở web online ít nhất một lần để cache HTML/JS/CSS/assets và trace data.
- Nếu QR chưa từng được cache thì offline sẽ không có dữ liệu để hiển thị.
- Các API `/api/...` không bị cache cứng; dữ liệu nghiệp vụ offline được quản lý bằng IndexedDB.
- Khi online lại, hệ thống sẽ tự refresh trace cache để lấy dữ liệu mới nhất.

### Kiểm tra API cache khi online

Mở trình duyệt:

```text
http://localhost:8080/api/system/offline-trace-cache
```

Nếu API trả dữ liệu danh sách QR/trace thì cache offline có thể hoạt động.
