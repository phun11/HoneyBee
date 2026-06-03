package com.example.agritrace.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/**
 * QrService quản lý phần cốt lõi của QR truy xuất.
 *
 * Vai trò nghiệp vụ:
 * - Sinh token QR khó đoán cho từng lô sản phẩm.
 * - Tạo chữ ký HMAC-SHA256 để chống sửa URL/token/signature giả.
 * - Sinh ảnh QR PNG để in/dán lên sản phẩm hoặc hiển thị trong UI.
 *
 * Lưu ý an toàn:
 * - QR KHÔNG chứa toàn bộ dữ liệu sản phẩm. QR chỉ chứa URL + token + sig.
 * - Dữ liệu thật được lấy từ DB qua Trace API để luôn mới và kiểm soát được.
 * - HMAC không chống việc chụp/in lại QR thật, nên DB còn có QR_SCAN_LOGS và QR_SECURITY_ALERTS.
 */
@Service
public class QrService {
    private final JdbcTemplate jdbc;

    /** Base URL frontend được encode vào QR. Khi test mobile LAN, đặt thành http://IP-LAPTOP:5500. */
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    /** Secret chỉ nằm ở backend/server. Không đưa key này xuống frontend hoặc QR. */
    @Value("${app.qr.secret}")
    private String qrSecret;

    public QrService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Tạo QR cho một lô sản phẩm.
     *
     * Quy trình:
     * 1) Kiểm tra lô đã có QR ACTIVE chưa để tránh tạo trùng.
     * 2) Sinh QR_TOKEN random bằng UUID, khó đoán hơn PRODUCT_ID tuần tự.
     * 3) Ký token bằng HMAC-SHA256: HMAC(token|productId, secret).
     * 4) Lưu QR_URL, QR_TOKEN, QR_SIGNATURE vào QR_CODES.
     * 5) Ghi AUDIT_LOGS để chứng minh QR được phát hành bởi hệ thống.
     */
    public String createQrRecord(Long productId) {
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM QR_CODES WHERE PRODUCT_ID=? AND STATUS='ACTIVE'",
                Integer.class,
                productId
        );
        if (existing != null && existing > 0) {
            return jdbc.queryForObject(
                    "SELECT QR_TOKEN FROM QR_CODES WHERE PRODUCT_ID=? AND STATUS='ACTIVE'",
                    String.class,
                    productId
            );
        }

        String token = "HB-QR-" + productId + "-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
        String signature = sign(token, productId);
        String url = frontendBaseUrl + "/product-detail.html?token=" + token + "&sig=" + signature;

        jdbc.update(
                "INSERT INTO QR_CODES(PRODUCT_ID, QR_TOKEN, QR_SIGNATURE, QR_URL, QR_IMAGE_URL, STATUS) VALUES(?,?,?,?,?,?)",
                productId,
                token,
                signature,
                url,
                "/api/qr/product/" + productId + "/image",
                "ACTIVE"
        );
        jdbc.update(
                "INSERT INTO AUDIT_LOGS(TABLE_NAME, RECORD_ID, ACTION_TYPE, NEW_DATA, PERFORMED_BY) VALUES(?,?,?,?,?)",
                "QR_CODES",
                String.valueOf(productId),
                "INSERT",
                "Phát hành QR token=" + token,
                "system"
        );
        return token;
    }

    /**
     * Sinh ảnh QR PNG cho sản phẩm.
     *
     * Ảnh QR lấy URL từ DB, không tự dựng lại ở frontend.
     * Cách này giúp khi đổi base-url hoặc revoke QR, backend vẫn kiểm soát được dữ liệu gốc.
     */
    public byte[] generateQrPngByProduct(Long productId) throws WriterException, IOException {
        String url = jdbc.queryForObject(
                "SELECT QR_URL FROM QR_CODES WHERE PRODUCT_ID=? AND STATUS IN ('ACTIVE','SUSPICIOUS')",
                String.class,
                productId
        );
        return generate(url);
    }

    /** Tạo QR PNG từ chuỗi bất kỳ; dùng cho test nhanh hoặc QR nội bộ. */
    public byte[] generate(String text) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 360, 360);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Tạo chữ ký HMAC-SHA256 cho QR.
     *
     * Ý nghĩa:
     * - Nếu người xấu sửa token/productId/sig trong URL, backend phát hiện chữ ký không khớp.
     * - HMAC là chữ ký xác thực, không phải mã hóa hai chiều.
     * - Secret key phải được bảo vệ ở server/backend.
     */
    private String sign(String token, Long productId) {
        try {
            String payload = token + "|" + productId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(qrSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign QR token", e);
        }
    }
}
