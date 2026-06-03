# System Analysis

## Mục tiêu
Hệ thống cho phép người tiêu dùng quét QR hoặc upload ảnh QR để truy xuất nguồn gốc nông sản sạch. Admin có thể quản lý nông sản, trang trại, lịch sử vận chuyển và dữ liệu timeline.

## Kiến trúc
- Frontend: HTML, CSS, JavaScript thuần.
- Backend: Java Spring Boot REST API.
- Database: Oracle SQL.

## Luồng người dùng
1. Người dùng vào trang scanner.
2. Hệ thống đọc QR bằng JavaScript `jsQR`.
3. Nếu QR chứa URL, trình duyệt redirect đến trang chi tiết.
4. Trang chi tiết gọi `GET /api/trace/product/{id}`.
5. Backend đọc view `VW_PRODUCT_TRACEABILITY`, bảng `TRANSPORT_HISTORY`, `PRODUCT_UPDATES`.
6. Frontend hiển thị thông tin sản phẩm, trang trại, nguồn gốc, vận chuyển và timeline.

## Luồng admin
1. Admin mở dashboard.
2. Admin thêm sản phẩm.
3. Backend gọi procedure `PRC_ADD_PRODUCT` để thêm sản phẩm và origin trong cùng transaction.
4. Backend tạo record QR trong bảng `QR_CODES`.
5. Admin mở chi tiết để lấy QR demo.

## Database nâng cao
Database có sequence, trigger auto ID, trigger auto timestamp, procedure thêm sản phẩm, function tính số ngày sau thu hoạch, package tổng hợp, view truy xuất và các script demo transaction/concurrency.
