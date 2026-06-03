# Nội dung đã sửa trong Oracle Business Version

- Chuẩn hóa `PRODUCTS` thành bảng lô hàng trung tâm: batch code, số lượng, đơn vị, store nhận, transporter phụ trách, điểm lấy, điểm giao, yêu cầu nhiệt độ/độ ẩm, ghi chú vận chuyển, người nhận, thời gian giao dự kiến.
- Bổ sung dữ liệu nguồn gốc trong `PRODUCT_ORIGIN`: địa chỉ farm, hạn sử dụng, độ tươi.
- Bổ sung chứng chỉ gắn theo lô trong `CERTIFICATES` và cảnh báo nếu thiếu/hết hạn.
- Mở rộng `TRANSPORT_HISTORY`: current location, nhiệt độ, độ ẩm, seal status, issue note, mỗi lần cập nhật insert dòng mới.
- Mở rộng `DISTRIBUTION_HISTORY`: lý do từ chối/trả hàng.
- Chuẩn hóa trạng thái lô: CREATED, FARM_CONFIRMED, WAITING_FOR_PICKUP, PICKED_UP, IN_TRANSIT, ARRIVED_AT_HUB, OUT_FOR_DELIVERY, DELIVERED, STORE_RECEIVED, AVAILABLE_FOR_SALE, RETURN_REQUESTED, RETURNED, CANCELLED, REJECTED, SOLD_OUT, EXPIRED, REVOKED.
- Sửa Farm form: chia nhóm sản phẩm, nông trại, chứng chỉ, chuyển giao; có import Excel theo field mới.
- Sửa Transport UI: chia danh sách lô theo trạng thái, xem rõ lấy/giao/Store/điều kiện vận chuyển, có form cập nhật chặng, báo sự cố, đã giao.
- Sửa Store UI: xem lô đã giao, xác nhận nhận hàng, đưa lên bán, từ chối/trả hàng, cập nhật trạng thái QR đã bán.
- Sửa Trace UI: hiển thị thông tin lô, Farm, Store, chứng chỉ, lịch sử trạng thái, chặng vận chuyển, phân phối và cảnh báo.
- Bổ sung API: route step, issue report, store reject, shipment history.
- Sửa SQL seed có `SET DEFINE OFF` để không bị lỗi `&sig` trong SQL Developer.


## V3 SQLPlus seed fix
- Fixed BATCH_STATUS_HISTORY seed inserts: removed extra DEFAULT value and used explicit column lists.
- Seed reset should now complete through 03_seed_demo_data.sql and populate 10 products, 8 users, 10 QR codes.
