package com.example.agritrace.repository;

import com.example.agritrace.dto.ProductRequest;
import com.example.agritrace.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
public class ProductRepository {
    private final JdbcTemplate jdbc;
    private final AuditRepository audit;

    public ProductRepository(JdbcTemplate jdbc, AuditRepository audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    private final RowMapper<Product> productMapper = (rs, i) -> {
        Product p = new Product();
        p.productId = rs.getLong("PRODUCT_ID");
        p.farmId = rs.getLong("FARM_ID");
        p.batchCode = rs.getString("BATCH_CODE");
        p.productName = rs.getString("PRODUCT_NAME");
        p.category = rs.getString("CATEGORY");
        p.description = rs.getString("DESCRIPTION");
        p.price = rs.getBigDecimal("PRICE");
        p.quantity = rs.getBigDecimal("QUANTITY");
        p.unit = rs.getString("UNIT");
        p.status = rs.getString("STATUS");
        p.imageUrl = rs.getString("IMAGE_URL");
        try { p.productImageBase64 = rs.getString("PRODUCT_IMAGE_B64"); } catch (Exception ignored) {}
        try { p.productImageMime = rs.getString("PRODUCT_IMAGE_MIME"); } catch (Exception ignored) {}
        return p;
    };

    public List<Product> findAll() {
        return jdbc.query("SELECT * FROM PRODUCTS ORDER BY PRODUCT_ID DESC", productMapper);
    }

    public Product findById(Long id) {
        return jdbc.queryForObject("SELECT * FROM PRODUCTS WHERE PRODUCT_ID = ?", productMapper, id);
    }

    @Transactional
    public Long createByProcedure(ProductRequest r) {
        if (r.farmId == null) throw new IllegalArgumentException("farmId is required");
        if (r.productName == null || r.productName.isBlank()) throw new IllegalArgumentException("productName is required");
        if (r.category == null || r.category.isBlank()) throw new IllegalArgumentException("category is required");
        if (r.storeId == null) throw new IllegalArgumentException("storeId is required");

        Long productId = jdbc.queryForObject("SELECT SEQ_PRODUCTS.NEXTVAL FROM dual", Long.class);
        String batchCode = (r.batchCode == null || r.batchCode.isBlank()) ? "HB-BATCH-" + productId : r.batchCode.trim();
        String unit = (r.unit == null || r.unit.isBlank()) ? "kg" : r.unit.trim();
        String pickup = firstNonBlank(r.pickupLocation, r.farmAddress, jdbc.queryForObject("SELECT ADDRESS FROM FARMS WHERE FARM_ID=?", String.class, r.farmId));
        String delivery = firstNonBlank(r.deliveryLocation, jdbc.queryForObject("SELECT ADDRESS FROM STORES WHERE STORE_ID=?", String.class, r.storeId));

        jdbc.update("""
                INSERT INTO PRODUCTS(
                    PRODUCT_ID, FARM_ID, STORE_ID, TRANSPORTER_ID, BATCH_CODE, PRODUCT_NAME, CATEGORY, DESCRIPTION,
                    QUALITY_SUMMARY, PRICE, QUANTITY, UNIT, IMAGE_URL, PRODUCT_IMAGE_B64, PRODUCT_IMAGE_MIME, STATUS, PICKUP_LOCATION, DELIVERY_LOCATION,
                    REQUIRED_TEMP_MIN, REQUIRED_TEMP_MAX, REQUIRED_HUMIDITY_MIN, REQUIRED_HUMIDITY_MAX,
                    TRANSPORT_NOTE, RECEIVER_NAME, RECEIVER_PHONE, EXPECTED_DELIVERY_AT, CREATED_BY
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                productId, r.farmId, r.storeId, r.transporterId, batchCode, r.productName, r.category, r.description,
                firstNonBlank(r.qualitySummary, "Lo hang duoc tao trong he thong HoneyBee Trace."), r.price, r.quantity, unit, r.imageUrl, normalizeBase64(r.productImageBase64), firstNonBlank(r.productImageMime, "image/png"),
                "CREATED", pickup, delivery, r.requiredTempMin, r.requiredTempMax, r.requiredHumidityMin, r.requiredHumidityMax,
                r.transportNote, r.receiverName, r.receiverPhone,
                r.expectedDeliveryAt == null ? null : Timestamp.valueOf(r.expectedDeliveryAt), r.farmId);

        jdbc.update("""
                INSERT INTO PRODUCT_ORIGIN(PRODUCT_ID, CULTIVATION_PLACE, FARM_ADDRESS, SOWING_DATE, HARVEST_DATE, EXPIRED_DATE, PRODUCTION_PROCESS, FRESHNESS_STATUS)
                VALUES(?,?,?,?,?,?,?,?)
                """,
                productId, r.cultivationPlace, firstNonBlank(r.farmAddress, pickup),
                r.sowingDate == null ? null : Date.valueOf(r.sowingDate),
                r.harvestDate == null ? null : Date.valueOf(r.harvestDate),
                r.expiredDate == null ? null : Date.valueOf(r.expiredDate),
                r.productionProcess, firstNonBlank(r.freshnessStatus, "FRESH"));

        if (r.certificateName != null && !r.certificateName.isBlank()) {
            String certStatus = resolveCertificateStatus(r.certificateExpiredDate);
            jdbc.update("""
                    INSERT INTO CERTIFICATES(FARM_ID, PRODUCT_ID, CERTIFICATE_NAME, ISSUED_BY, ISSUE_DATE, EXPIRED_DATE, FILE_URL, STATUS)
                    VALUES(?,?,?,?,?,?,?,?)
                    """, r.farmId, productId, r.certificateName, r.certificateIssuer,
                    r.certificateIssueDate == null ? null : Date.valueOf(r.certificateIssueDate),
                    r.certificateExpiredDate == null ? null : Date.valueOf(r.certificateExpiredDate),
                    r.certificateFileUrl, certStatus);
        } else {
            jdbc.update("INSERT INTO QR_SECURITY_ALERTS(QR_TOKEN, PRODUCT_ID, ALERT_TYPE, ALERT_LEVEL, ALERT_MESSAGE) VALUES(NULL,?,?,?,?)",
                    productId, "MISSING_CERTIFICATE", "MEDIUM", "Lô hàng chưa có chứng chỉ an toàn thực phẩm được công khai.");
        }

        jdbc.update("INSERT INTO PRODUCT_UPDATES(PRODUCT_ID, UPDATE_TITLE, UPDATE_CONTENT, UPDATED_BY) VALUES(?,?,?,?)",
                productId, "Tạo lô hàng", "Farm tạo lô hàng và chờ xác nhận.", "farm#" + r.farmId);
        jdbc.update("INSERT INTO BATCH_STATUS_HISTORY(PRODUCT_ID, OLD_STATUS, NEW_STATUS, CHANGED_BY, NOTE) VALUES(?,?,?,?,?)",
                productId, null, "CREATED", r.farmId, "Farm tạo lô hàng mới.");

        String token = "HB-QR-" + productId + "-" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
        String signature = "DEMO_SIGNATURE_" + productId;
        String qrUrl = "http://localhost:8080/product-detail.html?token=" + token + "&sig=" + signature;
        jdbc.update("INSERT INTO QR_CODES(PRODUCT_ID, QR_TOKEN, QR_SIGNATURE, QR_URL, QR_IMAGE_URL, STATUS, SALE_STATUS) VALUES(?,?,?,?,?,?,?)",
                productId, token, signature, qrUrl, "/api/qr/product/" + productId + "/image", "ACTIVE", "NOT_SOLD");

        audit.log("PRODUCTS", String.valueOf(productId), "INSERT", null, r.productName, "farm#" + r.farmId, null);
        audit.log("QR_CODES", token, "INSERT", null, "Tạo QR cho lô hàng mới", "system", null);
        return productId;
    }

    public List<Map<String, Object>> findByFarm(Long farmId) {
        String sql = """
                SELECT p.*, f.FARM_NAME, f.ADDRESS AS FARM_ADDRESS_MASTER, s.STORE_NAME, s.ADDRESS AS STORE_ADDRESS,
                       t.TRANSPORTER_NAME, q.QR_TOKEN, q.QR_SIGNATURE, q.SALE_STATUS, q.STATUS AS QR_STATUS,
                       o.CULTIVATION_PLACE, o.SOWING_DATE, o.HARVEST_DATE, o.EXPIRED_DATE, o.PRODUCTION_PROCESS, o.FRESHNESS_STATUS,
                       (SELECT COUNT(*) FROM CERTIFICATES c WHERE c.PRODUCT_ID=p.PRODUCT_ID) AS CERT_COUNT
                FROM PRODUCTS p
                JOIN FARMS f ON p.FARM_ID=f.FARM_ID
                LEFT JOIN STORES s ON p.STORE_ID=s.STORE_ID
                LEFT JOIN TRANSPORTERS t ON p.TRANSPORTER_ID=t.TRANSPORTER_ID
                LEFT JOIN QR_CODES q ON p.PRODUCT_ID=q.PRODUCT_ID
                LEFT JOIN PRODUCT_ORIGIN o ON p.PRODUCT_ID=o.PRODUCT_ID
                """;
        if (farmId == null) return jdbc.queryForList(sql + " ORDER BY p.PRODUCT_ID DESC");
        return jdbc.queryForList(sql + " WHERE p.FARM_ID=? ORDER BY p.PRODUCT_ID DESC", farmId);
    }

    @Transactional
    public void markReadyForTransport(Long productId, Long userId, String note) {
        Map<String,Object> p = jdbc.queryForMap("SELECT STATUS, FARM_ID, STORE_ID, PICKUP_LOCATION, DELIVERY_LOCATION FROM PRODUCTS WHERE PRODUCT_ID=? FOR UPDATE", productId);
        String oldStatus = String.valueOf(p.get("STATUS"));
        if (!List.of("CREATED", "FARM_CONFIRMED").contains(oldStatus)) {
            throw new IllegalArgumentException("Chỉ lô CREATED/FARM_CONFIRMED mới được xác nhận chờ lấy hàng. Trạng thái hiện tại: " + oldStatus);
        }
        if (p.get("STORE_ID") == null || p.get("PICKUP_LOCATION") == null || p.get("DELIVERY_LOCATION") == null) {
            throw new IllegalArgumentException("Lô hàng thiếu store/điểm lấy/điểm giao, chưa thể chuyển sang chờ lấy hàng.");
        }
        updateProductStatus(productId, oldStatus, "WAITING_FOR_PICKUP", userId, firstNonBlank(note, "Farm xác nhận lô hợp lệ và chờ đơn vị vận chuyển lấy hàng."));
        jdbc.update("INSERT INTO PRODUCT_UPDATES(PRODUCT_ID, UPDATE_TITLE, UPDATE_CONTENT, UPDATED_BY) VALUES(?,?,?,?)",
                productId, "Farm xác nhận lô", "Lô hàng đã sẵn sàng bàn giao cho vận chuyển.", "user#" + userId);
    }

    public void update(Long id, ProductRequest r) {
        jdbc.update("""
                UPDATE PRODUCTS SET FARM_ID=?, STORE_ID=?, TRANSPORTER_ID=?, PRODUCT_NAME=?, CATEGORY=?, DESCRIPTION=?, QUALITY_SUMMARY=?, PRICE=?,
                    QUANTITY=?, UNIT=?, IMAGE_URL=?, PRODUCT_IMAGE_B64=?, PRODUCT_IMAGE_MIME=?, PICKUP_LOCATION=?, DELIVERY_LOCATION=?, REQUIRED_TEMP_MIN=?, REQUIRED_TEMP_MAX=?,
                    REQUIRED_HUMIDITY_MIN=?, REQUIRED_HUMIDITY_MAX=?, TRANSPORT_NOTE=?, RECEIVER_NAME=?, RECEIVER_PHONE=?,
                    EXPECTED_DELIVERY_AT=?, VERSION_NO=VERSION_NO+1, UPDATED_AT=CURRENT_TIMESTAMP
                WHERE PRODUCT_ID=?
                """, r.farmId, r.storeId, r.transporterId, r.productName, r.category, r.description, r.qualitySummary, r.price,
                r.quantity, firstNonBlank(r.unit, "kg"), r.imageUrl, normalizeBase64(r.productImageBase64), firstNonBlank(r.productImageMime, "image/png"), r.pickupLocation, r.deliveryLocation, r.requiredTempMin, r.requiredTempMax,
                r.requiredHumidityMin, r.requiredHumidityMax, r.transportNote, r.receiverName, r.receiverPhone,
                r.expectedDeliveryAt == null ? null : Timestamp.valueOf(r.expectedDeliveryAt), id);
        jdbc.update("""
                UPDATE PRODUCT_ORIGIN SET CULTIVATION_PLACE=?, FARM_ADDRESS=?, SOWING_DATE=?, HARVEST_DATE=?, EXPIRED_DATE=?, PRODUCTION_PROCESS=?, FRESHNESS_STATUS=?, UPDATED_AT=CURRENT_TIMESTAMP
                WHERE PRODUCT_ID=?
                """, r.cultivationPlace, r.farmAddress,
                r.sowingDate == null ? null : Date.valueOf(r.sowingDate),
                r.harvestDate == null ? null : Date.valueOf(r.harvestDate),
                r.expiredDate == null ? null : Date.valueOf(r.expiredDate),
                r.productionProcess, firstNonBlank(r.freshnessStatus, "FRESH"), id);
        audit.log("PRODUCTS", String.valueOf(id), "UPDATE", null, r.productName, "system", null);
    }

    public void delete(Long id) {
        String oldStatus = jdbc.queryForObject("SELECT STATUS FROM PRODUCTS WHERE PRODUCT_ID=?", String.class, id);
        updateProductStatus(id, oldStatus, "CANCELLED", null, "Hủy mềm lô hàng, giữ lại lịch sử truy xuất.");
    }

    public void updateProductStatus(Long productId, String oldStatus, String newStatus, Long userId, String note) {
        jdbc.update("UPDATE PRODUCTS SET STATUS=?, VERSION_NO=VERSION_NO+1, UPDATED_AT=CURRENT_TIMESTAMP WHERE PRODUCT_ID=?", newStatus, productId);
        jdbc.update("INSERT INTO BATCH_STATUS_HISTORY(PRODUCT_ID, OLD_STATUS, NEW_STATUS, CHANGED_BY, NOTE) VALUES(?,?,?,?,?)",
                productId, oldStatus, newStatus, userId, note);
        audit.log("PRODUCTS", String.valueOf(productId), "STATUS_CHANGE", oldStatus, newStatus, userId == null ? "system" : "user#" + userId, null);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }

    private String normalizeBase64(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        int comma = v.indexOf(',');
        if (v.startsWith("data:") && comma >= 0) return v.substring(comma + 1);
        return v;
    }

    private String resolveCertificateStatus(java.time.LocalDate expiredDate) {
        if (expiredDate == null) return "VALID";
        java.time.LocalDate now = java.time.LocalDate.now();
        if (expiredDate.isBefore(now)) return "EXPIRED";
        if (!expiredDate.isAfter(now.plusDays(30))) return "EXPIRING_SOON";
        return "VALID";
    }
}
