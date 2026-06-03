package com.example.agritrace.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository phục vụ luồng khách quét QR.
 *
 * Điểm DBMS chính:
 * - Ghi QR_SCAN_LOGS trong cùng transaction với việc đánh giá rủi ro.
 * - Nếu phát hiện bất thường, ghi QR_SECURITY_ALERTS và AUDIT_LOGS.
 * - Truy vấn QR theo QR_TOKEN sử dụng UNIQUE INDEX IDX_QR_TOKEN.
 */
@Repository
public class TraceRepository {
    private final JdbcTemplate jdbc;
    private final AuditRepository audit;

    public TraceRepository(JdbcTemplate jdbc, AuditRepository audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    /** Truy xuất theo productId, dùng cho admin/demo nội bộ. */
    public Map<String, Object> trace(Long productId) {
        Map<String, Object> info = jdbc.queryForMap("SELECT * FROM VW_PUBLIC_TRACE WHERE PRODUCT_ID=?", productId);
        return buildTracePayload(info, null, List.of(), "NONE");
    }

    /** Tìm productId từ token, dùng cho scanner khi QR chỉ chứa token. */
    public Long productIdByToken(String token) {
        return jdbc.queryForObject("SELECT PRODUCT_ID FROM QR_CODES WHERE QR_TOKEN=?", Long.class, token);
    }

    /**
     * API public chính: quét QR -> ghi log -> đánh giá tin cậy -> trả payload mobile-first.
     *
     * Transaction note:
     * - Nếu ghi scan log thành công nhưng tạo alert lỗi thì rollback cùng nhau.
     * - Điều này giữ tính nhất quán: scan bất thường phải đi kèm cảnh báo tương ứng.
     */
    @Transactional
    public Map<String, Object> traceByToken(String token, String sig, String ip, String ua) {
        try {
            Map<String, Object> qr = jdbc.queryForMap("SELECT * FROM QR_CODES WHERE QR_TOKEN=?", token);
            Long productId = ((Number) qr.get("PRODUCT_ID")).longValue();
            Map<String, Object> info = jdbc.queryForMap("SELECT * FROM VW_PUBLIC_TRACE WHERE PRODUCT_ID=?", productId);

            List<String> warnings = evaluateWarnings(token, info, qr);
            String resultStatus = resolveResultStatus(String.valueOf(qr.get("STATUS")), sig, String.valueOf(qr.get("QR_SIGNATURE")));
            if ("INVALID_SIGNATURE".equals(resultStatus)) {
                warnings.add("Chữ ký QR không khớp với token trong hệ thống. Có thể URL QR đã bị sửa hoặc giả mạo.");
            }
            String warningLevel = resolveWarningLevel(warnings, info, qr, resultStatus);
            String warningMessage = warnings.isEmpty() ? "Không phát hiện bất thường." : String.join(" | ", warnings);

            jdbc.update("INSERT INTO QR_SCAN_LOGS(QR_TOKEN, PRODUCT_ID, IP_ADDRESS, USER_AGENT, RESULT_STATUS, WARNING_LEVEL, WARNING_MESSAGE) VALUES(?,?,?,?,?,?,?)",
                    token, productId, ip, ua, resultStatus, warningLevel, warningMessage);

            if (!warnings.isEmpty() || "REVOKED_QR".equals(resultStatus) || "EXPIRED_QR".equals(resultStatus) || "INVALID_SIGNATURE".equals(resultStatus)) {
                createQrAlert(token, productId, warningLevel, warningMessage);
            }

            audit.log("QR_SCAN_LOGS", String.valueOf(productId), "SCAN", null, resultStatus + " - " + warningMessage, "public", ip);
            return buildTracePayload(info, token, warnings, warningLevel);
        } catch (EmptyResultDataAccessException ex) {
            // Token không tồn tại vẫn được log để phục vụ phát hiện QR giả.
            jdbc.update("INSERT INTO QR_SCAN_LOGS(QR_TOKEN, RESULT_STATUS, IP_ADDRESS, USER_AGENT, WARNING_LEVEL, WARNING_MESSAGE) VALUES(?,?,?,?,?,?)",
                    token, "INVALID_TOKEN", ip, ua, "CRITICAL", "QR không thuộc hệ thống HoneyBee Trace.");
            audit.log("QR_SCAN_LOGS", token, "SCAN", null, "INVALID_TOKEN", "public", ip);
            return invalidTokenPayload(token);
        }
    }

    /** Đánh giá rủi ro QR dựa trên trạng thái QR, trạng thái lô và số lượt quét trong ngày. */
    private List<String> evaluateWarnings(String token, Map<String, Object> info, Map<String, Object> qr) {
        List<String> warnings = new ArrayList<>();
        String qrStatus = String.valueOf(qr.get("STATUS"));
        String qrSaleStatus = String.valueOf(qr.getOrDefault("SALE_STATUS", "NOT_SOLD"));
        String productStatus = String.valueOf(info.get("PRODUCT_STATUS"));
        String freshness = String.valueOf(info.get("FRESHNESS_STATUS"));

        if ("SUSPICIOUS".equals(qrStatus)) warnings.add("Mã QR đang bị đánh dấu nghi ngờ sao chép hoặc dùng sai vị trí.");
        if ("REVOKED".equals(qrStatus)) warnings.add("Mã QR đã bị thu hồi. Không nên mua sản phẩm này.");
        if ("EXPIRED".equals(qrStatus)) warnings.add("Mã QR đã hết hiệu lực.");
        if ("SOLD".equals(qrSaleStatus)) warnings.add("QR/lô hàng này đã được cửa hàng ghi nhận là đã bán. Nếu tem này đang dán trên sản phẩm khác, cần kiểm tra nguy cơ sao chép QR.");
        if ("SOLD_OUT".equals(productStatus)) warnings.add("Lô hàng đã được đánh dấu bán hết nhưng vẫn có lượt quét mới.");
        if ("EXPIRED".equals(freshness)) warnings.add("Lô hàng đã quá hạn sử dụng theo dữ liệu hệ thống.");
        if ("NEAR_EXPIRED".equals(freshness)) warnings.add("Sản phẩm gần hết hạn, hãy kiểm tra kỹ trước khi mua.");

        Long productId = ((Number) info.get("PRODUCT_ID")).longValue();
        Long certCount = jdbc.queryForObject("SELECT COUNT(*) FROM CERTIFICATES WHERE PRODUCT_ID=?", Long.class, productId);
        Long expiredCertCount = jdbc.queryForObject("SELECT COUNT(*) FROM CERTIFICATES WHERE PRODUCT_ID=? AND (STATUS='EXPIRED' OR (EXPIRED_DATE IS NOT NULL AND EXPIRED_DATE < TRUNC(CURRENT_DATE)))", Long.class, productId);
        if (certCount == null || certCount == 0) warnings.add("Lô hàng chưa có chứng chỉ an toàn thực phẩm được công khai.");
        if (expiredCertCount != null && expiredCertCount > 0) warnings.add("Chứng chỉ an toàn thực phẩm của lô hàng đã hết hạn.");

        Long todayScans = jdbc.queryForObject(
                "SELECT COUNT(*) FROM QR_SCAN_LOGS WHERE QR_TOKEN=? AND TRUNC(CAST(SCANNED_AT AS DATE))=TRUNC(CURRENT_DATE)", Long.class, token);
        // Quy tắc demo mới:
        // - 0 -> 40 lượt quét/ngày: SAFE nếu không có điều kiện rủi ro khác.
        // - Lượt quét thứ 41 trở đi: WARNING màu vàng.
        // Vì hàm này chạy trước khi ghi log lượt quét hiện tại, todayScans >= 40 nghĩa là sau lượt quét này sẽ > 40.
        if (todayScans != null && todayScans >= 40) {
            warnings.add("QR có hơn 40 lượt quét trong ngày. Cần kiểm tra khả năng tem QR bị chia sẻ/chụp lại.");
        }
        return warnings;
    }

    /** Phân loại mức cảnh báo để UI hiển thị màu xanh/cam/đỏ theo rule mới. */
    private String resolveWarningLevel(List<String> warnings, Map<String, Object> info, Map<String, Object> qr, String resultStatus) {
        String qrStatus = String.valueOf(qr.get("STATUS"));
        String qrSaleStatus = String.valueOf(qr.getOrDefault("SALE_STATUS", "NOT_SOLD"));
        String productStatus = String.valueOf(info.get("PRODUCT_STATUS"));
        String freshness = String.valueOf(info.get("FRESHNESS_STATUS"));

        // Đỏ/DANGER chỉ khi có điều kiện rủi ro đi kèm: đã bán/bán hết, hết hạn, thu hồi, sai chữ ký...
        if ("INVALID_SIGNATURE".equals(resultStatus)) return "HIGH";
        if ("REVOKED".equals(qrStatus) || "EXPIRED".equals(qrStatus)) return "HIGH";
        if ("SOLD".equals(qrSaleStatus) || "SOLD_OUT".equals(productStatus)) return "HIGH";
        if ("EXPIRED".equals(freshness)) return "HIGH";
        for (String w : warnings) {
            if (w.contains("chứng chỉ") && w.contains("hết hạn")) return "HIGH";
            if (w.contains("Niêm phong") || w.contains("từ chối") || w.contains("sự cố nghiêm trọng")) return "HIGH";
        }

        // Nghi ngờ, thiếu chứng chỉ hoặc >40 lượt/ngày chỉ là cảnh báo vàng nếu chưa có điều kiện nguy hiểm.
        if (!warnings.isEmpty()) return "MEDIUM";
        return "NONE";
    }

    /** Kiểm tra chữ ký QR demo. Dữ liệu mẫu dùng DEMO_SIGNATURE nên vẫn cho quét để demo. */
    private String resolveResultStatus(String qrStatus, String sig, String expectedSig) {
        if ("REVOKED".equals(qrStatus)) return "REVOKED_QR";
        if ("EXPIRED".equals(qrStatus)) return "EXPIRED_QR";
        if (sig != null && !sig.isBlank() && expectedSig != null && expectedSig.startsWith("DEMO_") == false && !sig.equals(expectedSig)) {
            return "INVALID_SIGNATURE";
        }
        return "SUCCESS";
    }

    /** Tạo cảnh báo QR và ghi audit. */
    private void createQrAlert(String token, Long productId, String level, String message) {
        jdbc.update("INSERT INTO QR_SECURITY_ALERTS(QR_TOKEN, PRODUCT_ID, ALERT_TYPE, ALERT_LEVEL, ALERT_MESSAGE) VALUES(?,?,?,?,?)",
                token, productId, "QR_SCAN_RISK", "NONE".equals(level) ? "LOW" : level, message);
        audit.log("QR_SECURITY_ALERTS", String.valueOf(productId), "QR_ALERT", null, message, "system", null);
    }

    /** Gom dữ liệu thành payload đúng với UI mobile-first. */
    private Map<String, Object> buildTracePayload(Map<String, Object> info, String token, List<String> warnings, String warningLevel) {
        Long productId = ((Number) info.get("PRODUCT_ID")).longValue();
        List<Map<String, Object>> transports = jdbc.queryForList("SELECT * FROM TRANSPORT_HISTORY WHERE PRODUCT_ID=? ORDER BY TRANSPORT_TIME", productId);
        List<Map<String, Object>> updates = jdbc.queryForList("SELECT * FROM PRODUCT_UPDATES WHERE PRODUCT_ID=? ORDER BY UPDATED_AT", productId);
        List<Map<String, Object>> certificates = jdbc.queryForList("SELECT * FROM CERTIFICATES WHERE PRODUCT_ID=? OR PRODUCT_ID IS NULL ORDER BY CERTIFICATE_ID", productId);
        List<Map<String, Object>> distributions = jdbc.queryForList("SELECT d.*, s.STORE_NAME, s.STORE_TYPE, s.ADDRESS AS STORE_ADDRESS, s.CITY AS STORE_CITY FROM DISTRIBUTION_HISTORY d JOIN STORES s ON d.STORE_ID=s.STORE_ID WHERE d.PRODUCT_ID=? ORDER BY d.RECEIVED_AT", productId);
        List<Map<String, Object>> statusHistory = jdbc.queryForList("SELECT * FROM BATCH_STATUS_HISTORY WHERE PRODUCT_ID=? ORDER BY CHANGED_AT, STATUS_HISTORY_ID", productId);
        List<Map<String, Object>> alerts = jdbc.queryForList("SELECT * FROM (SELECT * FROM QR_SECURITY_ALERTS WHERE PRODUCT_ID=? ORDER BY CREATED_AT DESC, ALERT_ID DESC) WHERE ROWNUM <= 5", productId);
        Long totalScan = jdbc.queryForObject("SELECT COUNT(*) FROM QR_SCAN_LOGS WHERE PRODUCT_ID=?", Long.class, productId);
        Long todayScan = token == null ? 0L : jdbc.queryForObject("SELECT COUNT(*) FROM QR_SCAN_LOGS WHERE QR_TOKEN=? AND TRUNC(CAST(SCANNED_AT AS DATE))=TRUNC(CURRENT_DATE)", Long.class, token);

        String trustLevel = "NONE".equals(warningLevel) ? "SAFE" : ("MEDIUM".equals(warningLevel) ? "WARNING" : "DANGER");
        String recommendation = switch (trustLevel) {
            case "SAFE" -> "BUY_OK";
            case "WARNING" -> "CHECK_CAREFULLY";
            default -> "DO_NOT_BUY";
        };

        Map<String, Object> trustStatus = new LinkedHashMap<>();
        trustStatus.put("level", trustLevel);
        trustStatus.put("title", "SAFE".equals(trustLevel) ? "QR hợp lệ" : ("WARNING".equals(trustLevel) ? "QR cần kiểm tra" : "Không nên mua"));
        trustStatus.put("message", warnings.isEmpty() ? "Không phát hiện dấu hiệu bất thường. Sản phẩm được xác thực bởi HoneyBee Trace." : String.join(" ", warnings));
        trustStatus.put("totalScanCount", totalScan);
        trustStatus.put("todayScanCount", todayScan);
        trustStatus.put("qrSaleStatus", info.get("QR_SALE_STATUS"));
        trustStatus.put("soldAt", info.get("SOLD_AT"));
        trustStatus.put("warnings", warnings);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("trustStatus", trustStatus);
        payload.put("recommendation", Map.of("level", recommendation, "message", recommendationMessage(recommendation)));
        payload.put("info", info);
        payload.put("transports", transports);
        payload.put("updates", updates);
        payload.put("certificates", certificates);
        payload.put("distributions", distributions);
        payload.put("statusHistory", statusHistory);
        payload.put("securityAlerts", alerts);
        payload.put("journey", buildJourney(info, transports, distributions, statusHistory));
        return payload;
    }

    /** Timeline cho khách hàng: dễ hiểu hơn bảng dữ liệu thô. */
    private List<Map<String, Object>> buildJourney(Map<String, Object> info, List<Map<String, Object>> transports, List<Map<String, Object>> distributions, List<Map<String, Object>> statusHistory) {
        List<Map<String, Object>> journey = new ArrayList<>();
        journey.add(Map.of("type", "FARM", "icon", "🌱", "title", "Nguồn gốc nông trại", "time", String.valueOf(info.get("HARVEST_DATE")), "description", String.valueOf(info.get("FARM_NAME")) + " - " + String.valueOf(info.get("CULTIVATION_PLACE"))));
        for (Map<String, Object> t : transports) {
            journey.add(Map.of("type", "TRANSPORT", "icon", "🚚", "title", "Vận chuyển", "time", String.valueOf(t.get("TRANSPORT_TIME")), "description", t.get("FROM_LOCATION") + " → " + t.get("TO_LOCATION")));
        }
        for (Map<String, Object> d : distributions) {
            journey.add(Map.of("type", "STORE", "icon", "🏪", "title", "Cửa hàng nhận hàng", "time", String.valueOf(d.get("RECEIVED_AT")), "description", String.valueOf(d.get("STORE_NAME"))));
        }
        return journey;
    }

    private String recommendationMessage(String level) {
        return switch (level) {
            case "BUY_OK" -> "Sản phẩm phù hợp để mua. Hãy kiểm tra tem còn nguyên vẹn trước khi thanh toán.";
            case "CHECK_CAREFULLY" -> "Có cảnh báo. Nên kiểm tra tem, nơi bán, hạn sử dụng và trạng thái đã bán/chưa bán.";
            default -> "Không nên mua sản phẩm này cho đến khi cửa hàng/HoneyBee xác minh lại.";
        };
    }

    private Map<String, Object> invalidTokenPayload(String token) {
        return Map.of(
                "trustStatus", Map.of(
                        "level", "DANGER",
                        "title", "QR không hợp lệ",
                        "message", "Mã QR không thuộc hệ thống HoneyBee Trace hoặc đã bị giả mạo.",
                        "warnings", List.of("INVALID_TOKEN")
                ),
                "recommendation", Map.of("level", "DO_NOT_BUY", "message", "Không nên mua sản phẩm này."),
                "info", Map.of("QR_TOKEN", token),
                "journey", List.of(),
                "certificates", List.of(),
                "transports", List.of(),
                "distributions", List.of(),
                "statusHistory", List.of(),
                "securityAlerts", List.of()
        );
    }
}
