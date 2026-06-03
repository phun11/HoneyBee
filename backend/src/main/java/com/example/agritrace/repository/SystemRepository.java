package com.example.agritrace.repository;

import com.example.agritrace.dto.CertificateRequest;
import com.example.agritrace.dto.StoreReceiveRequest;
import com.example.agritrace.dto.StoreSaleRequest;
import com.example.agritrace.dto.TransportRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SystemRepository {
    private final JdbcTemplate jdbc;
    private final AuditRepository audit;

    public SystemRepository(JdbcTemplate jdbc, AuditRepository audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("products", jdbc.queryForObject("SELECT COUNT(*) FROM PRODUCTS", Long.class));
        m.put("waitingPickup", jdbc.queryForObject("SELECT COUNT(*) FROM PRODUCTS WHERE STATUS='WAITING_FOR_PICKUP'", Long.class));
        m.put("inTransit", jdbc.queryForObject("SELECT COUNT(*) FROM PRODUCTS WHERE STATUS IN ('PICKED_UP','IN_TRANSIT','ARRIVED_AT_HUB','OUT_FOR_DELIVERY')", Long.class));
        m.put("delivered", jdbc.queryForObject("SELECT COUNT(*) FROM PRODUCTS WHERE STATUS IN ('DELIVERED','STORE_RECEIVED','AVAILABLE_FOR_SALE')", Long.class));
        m.put("problem", jdbc.queryForObject("SELECT COUNT(*) FROM PRODUCTS WHERE STATUS IN ('RETURN_REQUESTED','RETURNED','REJECTED','CANCELLED')", Long.class));
        m.put("farms", jdbc.queryForObject("SELECT COUNT(*) FROM FARMS", Long.class));
        m.put("transporters", jdbc.queryForObject("SELECT COUNT(*) FROM TRANSPORTERS", Long.class));
        m.put("stores", jdbc.queryForObject("SELECT COUNT(*) FROM STORES", Long.class));
        m.put("scanLogs", jdbc.queryForObject("SELECT COUNT(*) FROM QR_SCAN_LOGS", Long.class));
        m.put("securityAlerts", jdbc.queryForObject("SELECT COUNT(*) FROM QR_SECURITY_ALERTS WHERE RESOLVED_STATUS='OPEN'", Long.class));
        m.put("auditLogs", jdbc.queryForObject("SELECT COUNT(*) FROM AUDIT_LOGS", Long.class));
        return m;
    }

    public List<Map<String, Object>> users() { return jdbc.queryForList("SELECT USER_ID, USERNAME, FULL_NAME, ROLE, FARM_ID, TRANSPORTER_ID, STORE_ID, STATUS FROM USERS ORDER BY USER_ID"); }
    public List<Map<String, Object>> transporters() { return jdbc.queryForList("SELECT * FROM TRANSPORTERS ORDER BY TRANSPORTER_ID"); }
    public List<Map<String, Object>> stores() { return jdbc.queryForList("SELECT * FROM STORES ORDER BY STORE_ID"); }
    public List<Map<String, Object>> alerts() { return jdbc.queryForList("SELECT * FROM (SELECT * FROM QR_SECURITY_ALERTS ORDER BY CREATED_AT DESC, ALERT_ID DESC) WHERE ROWNUM <= 80"); }

    public List<Map<String, Object>> qrCodes() {
        return jdbc.queryForList("""
                SELECT p.PRODUCT_ID, p.BATCH_CODE, p.PRODUCT_NAME, p.CATEGORY, p.PRICE, p.QUANTITY, p.UNIT,
                       p.STATUS AS PRODUCT_STATUS, p.IMAGE_URL, p.PRODUCT_IMAGE_B64, p.PRODUCT_IMAGE_MIME, p.PICKUP_LOCATION, p.DELIVERY_LOCATION,
                       f.FARM_NAME, s.STORE_NAME, t.TRANSPORTER_NAME,
                       q.QR_ID, q.QR_TOKEN, q.QR_SIGNATURE, q.QR_URL, q.QR_IMAGE_URL,
                       q.STATUS AS QR_STATUS, q.SALE_STATUS, q.SOLD_AT, q.SOLD_NOTE, q.CREATED_AT
                FROM QR_CODES q
                JOIN PRODUCTS p ON q.PRODUCT_ID = p.PRODUCT_ID
                JOIN FARMS f ON p.FARM_ID=f.FARM_ID
                LEFT JOIN STORES s ON p.STORE_ID=s.STORE_ID
                LEFT JOIN TRANSPORTERS t ON p.TRANSPORTER_ID=t.TRANSPORTER_ID
                ORDER BY p.PRODUCT_ID DESC
                """);
    }

    public List<Map<String, Object>> certificates(Long productId) {
        if (productId == null) return jdbc.queryForList("SELECT * FROM CERTIFICATES ORDER BY CERTIFICATE_ID DESC");
        return jdbc.queryForList("SELECT * FROM CERTIFICATES WHERE PRODUCT_ID=? ORDER BY CERTIFICATE_ID DESC", productId);
    }

    public void addCertificate(CertificateRequest r) {
        String status = certificateStatus(r.expiredDate, r.status);
        jdbc.update("INSERT INTO CERTIFICATES(FARM_ID, PRODUCT_ID, CERTIFICATE_NAME, ISSUED_BY, ISSUE_DATE, EXPIRED_DATE, FILE_URL, STATUS) VALUES(?,?,?,?,?,?,?,?)",
                r.farmId, r.productId, r.certificateName, r.issuedBy,
                r.issueDate == null ? null : Date.valueOf(r.issueDate),
                r.expiredDate == null ? null : Date.valueOf(r.expiredDate),
                r.fileUrl, status);
        audit.log("CERTIFICATES", String.valueOf(r.productId), "INSERT", null, r.certificateName, "system", null);
    }

    public List<Map<String, Object>> storeProducts(Long storeId) {
        String sql = """
                SELECT p.PRODUCT_ID, p.BATCH_CODE, p.PRODUCT_NAME, p.CATEGORY, p.QUANTITY, p.UNIT, p.STATUS AS PRODUCT_STATUS, p.IMAGE_URL, p.PRODUCT_IMAGE_B64, p.PRODUCT_IMAGE_MIME,
                       p.PICKUP_LOCATION, p.DELIVERY_LOCATION, p.RECEIVER_NAME, p.RECEIVER_PHONE,
                       f.FARM_NAME, f.ADDRESS AS FARM_ADDRESS,
                       s.STORE_ID, s.STORE_NAME, s.ADDRESS AS STORE_ADDRESS,
                       q.QR_TOKEN, q.STATUS AS QR_STATUS, q.SALE_STATUS, q.SOLD_AT, q.SOLD_NOTE,
                       (SELECT COUNT(*) FROM TRANSPORT_HISTORY th WHERE th.PRODUCT_ID=p.PRODUCT_ID) AS TRANSPORT_STEP_COUNT
                FROM PRODUCTS p
                JOIN FARMS f ON p.FARM_ID=f.FARM_ID
                LEFT JOIN STORES s ON p.STORE_ID=s.STORE_ID
                LEFT JOIN QR_CODES q ON p.PRODUCT_ID=q.PRODUCT_ID
                WHERE p.STATUS IN ('DELIVERED','STORE_RECEIVED','AVAILABLE_FOR_SALE','RETURN_REQUESTED','RETURNED','REJECTED','SOLD_OUT')
                """;
        if (storeId == null) return jdbc.queryForList(sql + " ORDER BY p.PRODUCT_ID DESC");
        return jdbc.queryForList(sql + " AND p.STORE_ID=? ORDER BY p.PRODUCT_ID DESC", storeId);
    }

    @Transactional
    public void markQrSaleStatus(StoreSaleRequest r) {
        String saleStatus = r.saleStatus == null ? "SOLD" : r.saleStatus.toUpperCase();
        if (!List.of("NOT_SOLD", "SOLD", "RETURNED").contains(saleStatus)) throw new IllegalArgumentException("saleStatus must be NOT_SOLD, SOLD or RETURNED");
        Map<String, Object> qr = r.qrToken != null && !r.qrToken.isBlank()
                ? jdbc.queryForMap("SELECT * FROM QR_CODES WHERE QR_TOKEN=?", r.qrToken.trim())
                : jdbc.queryForMap("SELECT * FROM QR_CODES WHERE PRODUCT_ID=?", r.productId);
        Long productId = ((Number) qr.get("PRODUCT_ID")).longValue();
        String token = String.valueOf(qr.get("QR_TOKEN"));
        String oldStatus = jdbc.queryForObject("SELECT STATUS FROM PRODUCTS WHERE PRODUCT_ID=?", String.class, productId);

        if ("SOLD".equals(saleStatus)) {
            jdbc.update("UPDATE QR_CODES SET SALE_STATUS='SOLD', SOLD_AT=CURRENT_TIMESTAMP, SOLD_BY_STORE_ID=?, SOLD_NOTE=? WHERE QR_TOKEN=?", r.storeId, r.note, token);
            changeStatus(productId, oldStatus, "SOLD_OUT", r.userId, firstNonBlank(r.note, "Cửa hàng/POS đánh dấu QR đã bán."));
        } else {
            jdbc.update("UPDATE QR_CODES SET SALE_STATUS=?, SOLD_AT=NULL, SOLD_BY_STORE_ID=?, SOLD_NOTE=? WHERE QR_TOKEN=?", saleStatus, r.storeId, r.note, token);
            String newStatus = "RETURNED".equals(saleStatus) ? "RETURNED" : "AVAILABLE_FOR_SALE";
            changeStatus(productId, oldStatus, newStatus, r.userId, firstNonBlank(r.note, "Cửa hàng cập nhật lại trạng thái bán của QR."));
        }
        audit.log("QR_CODES", token, "STATUS_CHANGE", oldStatus, "SALE_STATUS=" + saleStatus, "store#" + r.storeId, null);
    }

    public List<Map<String, Object>> pendingShipments(Long transporterId) {
        String sql = baseShipmentSql() + """
                WHERE p.STATUS IN ('WAITING_FOR_PICKUP','PICKED_UP','IN_TRANSIT','ARRIVED_AT_HUB','OUT_FOR_DELIVERY','DELIVERED','RETURN_REQUESTED','RETURNED','REJECTED')
                """;
        if (transporterId == null) return jdbc.queryForList(sql + " ORDER BY CASE p.STATUS WHEN 'WAITING_FOR_PICKUP' THEN 1 WHEN 'PICKED_UP' THEN 2 WHEN 'IN_TRANSIT' THEN 3 WHEN 'ARRIVED_AT_HUB' THEN 4 WHEN 'OUT_FOR_DELIVERY' THEN 5 ELSE 6 END, p.PRODUCT_ID DESC");
        return jdbc.queryForList(sql + " AND (p.TRANSPORTER_ID=? OR p.TRANSPORTER_ID IS NULL OR p.STATUS='WAITING_FOR_PICKUP') ORDER BY CASE p.STATUS WHEN 'WAITING_FOR_PICKUP' THEN 1 WHEN 'PICKED_UP' THEN 2 WHEN 'IN_TRANSIT' THEN 3 WHEN 'ARRIVED_AT_HUB' THEN 4 WHEN 'OUT_FOR_DELIVERY' THEN 5 ELSE 6 END, p.PRODUCT_ID DESC", transporterId);
    }

    public List<Map<String, Object>> transportHistory(Long transporterId) {
        if (transporterId == null) return jdbc.queryForList("""
                SELECT th.*, p.BATCH_CODE, p.PRODUCT_NAME, t.TRANSPORTER_NAME
                FROM TRANSPORT_HISTORY th
                JOIN PRODUCTS p ON th.PRODUCT_ID=p.PRODUCT_ID
                LEFT JOIN TRANSPORTERS t ON th.TRANSPORTER_ID=t.TRANSPORTER_ID
                ORDER BY th.TRANSPORT_TIME DESC, th.TRANSPORT_ID DESC
                """);
        return jdbc.queryForList("""
                SELECT th.*, p.BATCH_CODE, p.PRODUCT_NAME, t.TRANSPORTER_NAME
                FROM TRANSPORT_HISTORY th
                JOIN PRODUCTS p ON th.PRODUCT_ID=p.PRODUCT_ID
                LEFT JOIN TRANSPORTERS t ON th.TRANSPORTER_ID=t.TRANSPORTER_ID
                WHERE th.TRANSPORTER_ID=?
                ORDER BY th.TRANSPORT_TIME DESC, th.TRANSPORT_ID DESC
                """, transporterId);
    }

    @Transactional
    public void pickupShipment(TransportRequest r) {
        Map<String,Object> p = jdbc.queryForMap("SELECT STATUS, TRANSPORTER_ID, PICKUP_LOCATION, DELIVERY_LOCATION FROM PRODUCTS WHERE PRODUCT_ID=? FOR UPDATE", r.productId);
        String oldStatus = String.valueOf(p.get("STATUS"));
        if (!"WAITING_FOR_PICKUP".equals(oldStatus)) throw new IllegalArgumentException("Chỉ lô WAITING_FOR_PICKUP mới được nhận hàng. Trạng thái hiện tại: " + oldStatus);
        String company = transportCompany(r.transporterId, r.transportCompany);
        insertTransportHistory(r, company, "PICKED_UP", firstNonBlank(r.note, "Đã nhận hàng tại điểm lấy."));
        jdbc.update("UPDATE PRODUCTS SET TRANSPORTER_ID=?, STATUS='PICKED_UP', VERSION_NO=VERSION_NO+1, UPDATED_AT=CURRENT_TIMESTAMP WHERE PRODUCT_ID=?", r.transporterId, r.productId);
        insertStatusHistory(r.productId, oldStatus, "PICKED_UP", r.userId, "Transporter nhận hàng.");
        audit.log("TRANSPORT_HISTORY", String.valueOf(r.productId), "STATUS_CHANGE", oldStatus, "PICKED_UP", "transport#" + r.transporterId, null);
    }

    @Transactional
    public void addTransport(TransportRequest r) {
        Map<String,Object> p = jdbc.queryForMap("SELECT STATUS FROM PRODUCTS WHERE PRODUCT_ID=? FOR UPDATE", r.productId);
        String oldStatus = String.valueOf(p.get("STATUS"));
        String newStatus = normalizeTransportStatus(r.status, oldStatus);
        String company = transportCompany(r.transporterId, r.transportCompany);
        insertTransportHistory(r, company, newStatus, firstNonBlank(r.note, "Cập nhật chặng vận chuyển."));
        changeStatus(r.productId, oldStatus, newStatus, r.userId, firstNonBlank(r.note, "Transport cập nhật chặng: " + newStatus));
        evaluateTransportAlert(r, newStatus);
    }

    @Transactional
    public void deliverShipment(TransportRequest r) {
        Map<String,Object> p = jdbc.queryForMap("SELECT STATUS FROM PRODUCTS WHERE PRODUCT_ID=? FOR UPDATE", r.productId);
        String oldStatus = String.valueOf(p.get("STATUS"));
        if (!List.of("PICKED_UP", "IN_TRANSIT", "ARRIVED_AT_HUB", "OUT_FOR_DELIVERY").contains(oldStatus)) {
            throw new IllegalArgumentException("Chỉ lô đang vận chuyển mới được xác nhận đã giao. Trạng thái hiện tại: " + oldStatus);
        }
        String company = transportCompany(r.transporterId, r.transportCompany);
        insertTransportHistory(r, company, "DELIVERED", firstNonBlank(r.note, "Đã giao đến điểm nhận."));
        changeStatus(r.productId, oldStatus, "DELIVERED", r.userId, firstNonBlank(r.note, "Transport xác nhận đã giao đến Store."));
        evaluateTransportAlert(r, "DELIVERED");
    }

    @Transactional
    public void reportIssue(TransportRequest r) {
        Map<String,Object> p = jdbc.queryForMap("SELECT STATUS FROM PRODUCTS WHERE PRODUCT_ID=? FOR UPDATE", r.productId);
        String oldStatus = String.valueOf(p.get("STATUS"));
        String company = transportCompany(r.transporterId, r.transportCompany);
        insertTransportHistory(r, company, "RETURN_REQUESTED", firstNonBlank(r.issueNote, r.note, "Transport báo sự cố/trả hàng."));
        changeStatus(r.productId, oldStatus, "RETURN_REQUESTED", r.userId, firstNonBlank(r.issueNote, r.note, "Transport báo sự cố/trả hàng."));
        createAlert(r.productId, "TRANSPORT_ISSUE", "HIGH", firstNonBlank(r.issueNote, r.note, "Transport báo sự cố nghiêm trọng."));
    }

    @Transactional
    public void confirmStoreReceive(StoreReceiveRequest r) {
        String oldStatus = jdbc.queryForObject("SELECT STATUS FROM PRODUCTS WHERE PRODUCT_ID=? FOR UPDATE", String.class, r.productId);
        if (!List.of("DELIVERED", "STORE_RECEIVED", "AVAILABLE_FOR_SALE", "RETURN_REQUESTED").contains(oldStatus)) {
            throw new IllegalArgumentException("Store chỉ nhận được lô đã DELIVERED. Trạng thái hiện tại: " + oldStatus);
        }
        String input = r.status == null ? "RECEIVED" : r.status.toUpperCase();
        String distStatus = "REJECTED".equals(input) ? "REJECTED" : ("AVAILABLE_FOR_SALE".equals(input) ? "AVAILABLE_FOR_SALE" : "STORE_RECEIVED");
        String productStatus = "REJECTED".equals(distStatus) ? "REJECTED" : distStatus;
        jdbc.update("INSERT INTO DISTRIBUTION_HISTORY(PRODUCT_ID, STORE_ID, RECEIVED_BY, QUANTITY_RECEIVED, STATUS, NOTE, REJECT_REASON) VALUES(?,?,?,?,?,?,?)",
                r.productId, r.storeId, r.userId, r.quantityReceived, distStatus, r.note, r.rejectReason);
        changeStatus(r.productId, oldStatus, productStatus, r.userId, firstNonBlank(r.note, r.rejectReason, "Store cập nhật trạng thái nhận hàng."));
        if ("REJECTED".equals(distStatus)) {
            createAlert(r.productId, "STORE_REJECTED", "HIGH", firstNonBlank(r.rejectReason, "Store từ chối nhận hàng."));
        }
    }

    private String baseShipmentSql() {
        return """
                SELECT p.PRODUCT_ID, p.BATCH_CODE, p.PRODUCT_NAME, p.CATEGORY, p.DESCRIPTION, p.STATUS,
                       p.QUANTITY, p.UNIT, p.IMAGE_URL, p.PRODUCT_IMAGE_B64, p.PRODUCT_IMAGE_MIME, p.PICKUP_LOCATION, p.DELIVERY_LOCATION, p.REQUIRED_TEMP_MIN, p.REQUIRED_TEMP_MAX,
                       p.REQUIRED_HUMIDITY_MIN, p.REQUIRED_HUMIDITY_MAX, p.TRANSPORT_NOTE, p.RECEIVER_NAME, p.RECEIVER_PHONE, p.EXPECTED_DELIVERY_AT,
                       f.FARM_NAME, f.ADDRESS AS FARM_ADDRESS, f.CONTACT_PHONE AS FARM_PHONE,
                       s.STORE_ID, s.STORE_NAME, s.ADDRESS AS STORE_ADDRESS, s.PHONE AS STORE_PHONE, s.CONTACT_PERSON AS STORE_CONTACT,
                       t.TRANSPORTER_ID, t.TRANSPORTER_NAME,
                       q.QR_TOKEN, q.QR_SIGNATURE
                FROM PRODUCTS p
                JOIN FARMS f ON p.FARM_ID=f.FARM_ID
                LEFT JOIN STORES s ON p.STORE_ID=s.STORE_ID
                LEFT JOIN TRANSPORTERS t ON p.TRANSPORTER_ID=t.TRANSPORTER_ID
                LEFT JOIN QR_CODES q ON p.PRODUCT_ID=q.PRODUCT_ID
                """;
    }

    private void insertTransportHistory(TransportRequest r, String company, String status, String note) {
        jdbc.update("""
                INSERT INTO TRANSPORT_HISTORY(PRODUCT_ID, TRANSPORTER_ID, TRANSPORT_COMPANY, FROM_LOCATION, TO_LOCATION, CURRENT_LOCATION,
                    STORAGE_TEMPERATURE, HUMIDITY, SEAL_STATUS, STATUS, NOTE, ISSUE_NOTE, CREATED_BY)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, r.productId, r.transporterId, company, r.fromLocation, r.toLocation, r.currentLocation,
                r.storageTemperature, r.humidity, firstNonBlank(r.sealStatus, "INTACT"), status, note, r.issueNote, r.userId);
        jdbc.update("INSERT INTO PRODUCT_UPDATES(PRODUCT_ID, UPDATE_TITLE, UPDATE_CONTENT, UPDATED_BY) VALUES(?,?,?,?)",
                r.productId, "Cập nhật vận chuyển", status + " - " + note, "transport#" + r.transporterId);
    }

    private void changeStatus(Long productId, String oldStatus, String newStatus, Long userId, String note) {
        jdbc.update("UPDATE PRODUCTS SET STATUS=?, VERSION_NO=VERSION_NO+1, UPDATED_AT=CURRENT_TIMESTAMP WHERE PRODUCT_ID=?", newStatus, productId);
        insertStatusHistory(productId, oldStatus, newStatus, userId, note);
    }

    private void insertStatusHistory(Long productId, String oldStatus, String newStatus, Long userId, String note) {
        jdbc.update("INSERT INTO BATCH_STATUS_HISTORY(PRODUCT_ID, OLD_STATUS, NEW_STATUS, CHANGED_BY, NOTE) VALUES(?,?,?,?,?)", productId, oldStatus, newStatus, userId, note);
        audit.log("PRODUCTS", String.valueOf(productId), "STATUS_CHANGE", oldStatus, newStatus, userId == null ? "system" : "user#" + userId, null);
    }

    private void evaluateTransportAlert(TransportRequest r, String status) {
        Map<String,Object> req = jdbc.queryForMap("SELECT REQUIRED_TEMP_MIN, REQUIRED_TEMP_MAX, REQUIRED_HUMIDITY_MIN, REQUIRED_HUMIDITY_MAX, DELIVERY_LOCATION, EXPECTED_DELIVERY_AT FROM PRODUCTS WHERE PRODUCT_ID=?", r.productId);
        if (r.storageTemperature != null) {
            BigDecimal min = (BigDecimal) req.get("REQUIRED_TEMP_MIN");
            BigDecimal max = (BigDecimal) req.get("REQUIRED_TEMP_MAX");
            if (min != null && r.storageTemperature.compareTo(min) < 0) createAlert(r.productId, "TEMP_TOO_LOW", "MEDIUM", "Nhiệt độ bảo quản thấp hơn yêu cầu: " + r.storageTemperature + "°C");
            if (max != null && r.storageTemperature.compareTo(max) > 0) createAlert(r.productId, "TEMP_TOO_HIGH", r.storageTemperature.subtract(max).abs().compareTo(BigDecimal.valueOf(5)) > 0 ? "HIGH" : "MEDIUM", "Nhiệt độ bảo quản vượt ngưỡng: " + r.storageTemperature + "°C");
        }
        if (r.humidity != null) {
            BigDecimal minH = (BigDecimal) req.get("REQUIRED_HUMIDITY_MIN");
            BigDecimal maxH = (BigDecimal) req.get("REQUIRED_HUMIDITY_MAX");
            if (minH != null && r.humidity.compareTo(minH) < 0) createAlert(r.productId, "HUMIDITY_TOO_LOW", "MEDIUM", "Độ ẩm thấp hơn yêu cầu: " + r.humidity + "%");
            if (maxH != null && r.humidity.compareTo(maxH) > 0) createAlert(r.productId, "HUMIDITY_TOO_HIGH", "MEDIUM", "Độ ẩm vượt yêu cầu: " + r.humidity + "%");
        }
        if (r.sealStatus != null && List.of("BROKEN", "DAMAGED", "OPENED").contains(r.sealStatus.toUpperCase())) createAlert(r.productId, "SEAL_BROKEN", "HIGH", "Niêm phong bị rách/mở.");
        if (r.issueNote != null && !r.issueNote.isBlank()) createAlert(r.productId, "TRANSPORT_NOTE", severityFromText(r.issueNote), r.issueNote);
        if ("DELIVERED".equals(status) && r.toLocation != null) {
            String delivery = String.valueOf(req.get("DELIVERY_LOCATION"));
            if (delivery != null && !delivery.equals("null") && !r.toLocation.toLowerCase().contains(delivery.toLowerCase().split(",")[0])) {
                createAlert(r.productId, "WRONG_DELIVERY_ROUTE", "HIGH", "Điểm giao thực tế có thể khác điểm giao dự kiến.");
            }
        }
    }

    private void createAlert(Long productId, String type, String level, String message) {
        String token = null;
        try { token = jdbc.queryForObject("SELECT QR_TOKEN FROM QR_CODES WHERE PRODUCT_ID=?", String.class, productId); } catch (Exception ignored) {}
        jdbc.update("INSERT INTO QR_SECURITY_ALERTS(QR_TOKEN, PRODUCT_ID, ALERT_TYPE, ALERT_LEVEL, ALERT_MESSAGE) VALUES(?,?,?,?,?)", token, productId, type, level, message);
        audit.log("QR_SECURITY_ALERTS", String.valueOf(productId), "QR_ALERT", null, message, "system", null);
    }

    private String normalizeTransportStatus(String requested, String oldStatus) {
        String s = requested == null ? "IN_TRANSIT" : requested.toUpperCase();
        if (List.of("IN_TRANSIT", "ARRIVED_AT_HUB", "OUT_FOR_DELIVERY", "RETURN_REQUESTED", "RETURNED", "REJECTED").contains(s)) return s;
        if ("DELIVERED".equals(s)) return "DELIVERED";
        return oldStatus.equals("PICKED_UP") ? "IN_TRANSIT" : oldStatus;
    }

    private String transportCompany(Long transporterId, String name) {
        if (name != null && !name.isBlank()) return name;
        if (transporterId == null) return "Đơn vị vận chuyển";
        return jdbc.queryForObject("SELECT TRANSPORTER_NAME FROM TRANSPORTERS WHERE TRANSPORTER_ID=?", String.class, transporterId);
    }

    private String certificateStatus(java.time.LocalDate expiredDate, String provided) {
        if (provided != null && !provided.isBlank()) return provided.toUpperCase();
        if (expiredDate == null) return "VALID";
        java.time.LocalDate now = java.time.LocalDate.now();
        if (expiredDate.isBefore(now)) return "EXPIRED";
        if (!expiredDate.isAfter(now.plusDays(30))) return "EXPIRING_SOON";
        return "VALID";
    }

    private String severityFromText(String text) {
        String s = text.toLowerCase();
        if (s.contains("rách") || s.contains("hỏng") || s.contains("nghiêm trọng") || s.contains("sai") || s.contains("mất")) return "HIGH";
        return "MEDIUM";
    }

    private String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }

    public Map<String, Object> health() {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("currentUser", jdbc.queryForObject("SELECT USER FROM dual", String.class));
        h.put("schemaVersion", tableExists("APP_SCHEMA_INFO") ? jdbc.queryForObject("SELECT MAX(VERSION_CODE) FROM APP_SCHEMA_INFO", String.class) : "UNKNOWN");
        h.put("productCount", safeCount("PRODUCTS"));
        h.put("farmCount", safeCount("FARMS"));
        h.put("storeCount", safeCount("STORES"));
        h.put("transporterCount", safeCount("TRANSPORTERS"));
        h.put("qrCount", safeCount("QR_CODES"));
        String[] tables = {"FARMS","TRANSPORTERS","STORES","USERS","PRODUCTS","PRODUCT_ORIGIN","CERTIFICATES","TRANSPORT_HISTORY","DISTRIBUTION_HISTORY","BATCH_STATUS_HISTORY","PRODUCT_UPDATES","QR_CODES","QR_SCAN_LOGS","QR_SECURITY_ALERTS","AUDIT_LOGS"};
        java.util.List<String> missingTables = new java.util.ArrayList<>();
        for (String t : tables) if (!tableExists(t)) missingTables.add(t);
        h.put("missingTables", missingTables);
        String[][] columns = {
                {"PRODUCTS","BATCH_CODE"},{"PRODUCTS","PRODUCT_IMAGE_B64"},{"PRODUCTS","PRODUCT_IMAGE_MIME"},{"PRODUCTS","STORE_ID"},{"PRODUCTS","TRANSPORTER_ID"},{"PRODUCTS","PICKUP_LOCATION"},{"PRODUCTS","DELIVERY_LOCATION"},{"PRODUCTS","REQUIRED_TEMP_MIN"},{"PRODUCTS","REQUIRED_HUMIDITY_MAX"},
                {"QR_CODES","SOLD_NOTE"},{"QR_CODES","SALE_STATUS"},{"TRANSPORT_HISTORY","CURRENT_LOCATION"},{"TRANSPORT_HISTORY","SEAL_STATUS"},{"STORES","STORE_NAME"}
        };
        java.util.List<String> missingColumns = new java.util.ArrayList<>();
        for (String[] c : columns) if (!columnExists(c[0], c[1])) missingColumns.add(c[0] + "." + c[1]);
        h.put("missingColumns", missingColumns);
        h.put("ready", missingTables.isEmpty() && missingColumns.isEmpty());
        return h;
    }

    private boolean tableExists(String table) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME=?", Long.class, table);
        return n != null && n > 0;
    }

    private boolean columnExists(String table, String column) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME=? AND COLUMN_NAME=?", Long.class, table, column);
        return n != null && n > 0;
    }

    private Long safeCount(String table) {
        try { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
        catch (Exception e) { return null; }
    }

}