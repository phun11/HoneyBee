package com.example.agritrace.dto;

/**
 * Request cửa hàng dùng để đánh dấu QR/lô hàng đã bán hoặc hoàn trả.
 *
 * Ý nghĩa QR security:
 * - Store nhập QR token thật trên tem sản phẩm khi bán hàng.
 * - Hệ thống ghi SALE_STATUS trực tiếp vào QR_CODES để biết QR này đã bán hay chưa.
 * - Nếu QR đã SOLD mà sau này vẫn bị quét ở sản phẩm khác, trang khách hàng sẽ cảnh báo.
 * - Trong tương lai, request này có thể được gửi tự động từ máy POS khi thanh toán.
 */
public class StoreSaleRequest {
    public String qrToken;
    public Long productId;
    public Long storeId;
    public Long userId;
    public String saleStatus;
    public String note;
}
