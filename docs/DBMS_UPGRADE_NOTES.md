# DBMS upgrade notes

## 1. Vai trò DBMS trong hệ thống

DBMS không chỉ lưu dữ liệu mà còn bảo vệ tính đúng đắn của chuỗi cung ứng:

- Toàn vẹn dữ liệu bằng PK, FK, UNIQUE, CHECK.
- Minh bạch bằng audit log, status history và scan log append-only.
- Kiểm soát đồng thời bằng `SELECT FOR UPDATE`, `VERSION_NO`, isolation level.
- Phục hồi bằng transaction rollback, undo log, redo log, checkpoint.
- Hiệu năng bằng index và execution plan.

## 2. Rủi ro QR bị sao chép

QR tĩnh có thể bị chụp/in lại và dán lên sản phẩm kém chất lượng. Hệ thống không thể chống 100% bằng phần mềm, nên thiết kế DB hỗ trợ phát hiện:

- `QR_CODES`: token, signature, status.
- `QR_SCAN_LOGS`: mọi lần quét.
- `QR_SECURITY_ALERTS`: cảnh báo scan bất thường.
- `AUDIT_LOGS`: ghi lại cảnh báo và thao tác revoke.

## 3. Lost Update

Cột `PRODUCTS.VERSION_NO` giúp phát hiện hai người cùng sửa trạng thái lô. Khi update, procedure kiểm tra version cũ. Nếu version đã đổi, transaction bị từ chối.

## 4. Deadlock

Quy tắc phòng tránh: lock theo thứ tự cố định:

1. PRODUCTS
2. QR_CODES
3. TRANSPORT_HISTORY
4. DISTRIBUTION_HISTORY
5. AUDIT_LOGS

## 5. Recovery

Nghiệp vụ tạo lô hàng và QR phải là một transaction. Nếu lỗi giữa chừng, rollback xóa toàn bộ dữ liệu dở dang. Oracle dùng undo/redo/checkpoint để phục hồi sau crash.
