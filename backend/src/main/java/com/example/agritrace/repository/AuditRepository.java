package com.example.agritrace.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Repository ghi và đọc audit log.
 *
 * DBMS note:
 * - Audit log là dữ liệu minh bạch, chỉ nên INSERT thêm dòng mới.
 * - Trong Oracle thật, trigger TRG_AUDIT_APPEND_ONLY sẽ chặn UPDATE/DELETE.
 * - Backend không cung cấp API xóa audit log để tránh che giấu lịch sử.
 */
@Repository
public class AuditRepository {
    private final JdbcTemplate jdbc;

    public AuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Ghi một dòng audit ngắn gọn cho hành động quan trọng. */
    public void log(String tableName, String recordId, String actionType, String oldData, String newData, String performedBy, String ip) {
        jdbc.update("""
                INSERT INTO AUDIT_LOGS(TABLE_NAME, RECORD_ID, ACTION_TYPE, OLD_DATA, NEW_DATA, PERFORMED_BY, IP_ADDRESS, SOURCE_TYPE)
                VALUES(?,?,?,?,?,?,?, 'ONLINE')
                """, tableName, recordId, actionType, oldData, newData, performedBy, ip);
    }

    /** Ghi audit cho thao tác được tạo khi offline và đồng bộ lại sau khi reconnect. */
    public void logOfflineSync(String tableName, String recordId, String actionType, String oldData, String newData, String performedBy,
                               String clientActionId, String deviceId, java.time.LocalDateTime offlineCreatedAt) {
        jdbc.update("""
                INSERT INTO AUDIT_LOGS(TABLE_NAME, RECORD_ID, ACTION_TYPE, OLD_DATA, NEW_DATA, PERFORMED_BY, IP_ADDRESS,
                    CLIENT_ACTION_ID, DEVICE_ID, OFFLINE_CREATED_AT, SYNCED_AT, SOURCE_TYPE)
                VALUES(?,?,?,?,?,?,NULL,?,?,?,CURRENT_TIMESTAMP,'OFFLINE_SYNC')
                """, tableName, recordId, actionType, oldData, newData, performedBy, clientActionId, deviceId,
                offlineCreatedAt == null ? null : java.sql.Timestamp.valueOf(offlineCreatedAt));
    }

    /**
     * Lấy audit mới nhất để admin kiểm tra nhanh trên dashboard.
     *
     * Dùng ROWNUM để tương thích Oracle XE/11g/12c+.
     * limit được giới hạn trong Java để tránh ghép SQL tùy ý từ input người dùng.
     */
    public List<Map<String, Object>> latest(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return jdbc.queryForList("SELECT * FROM (SELECT * FROM AUDIT_LOGS ORDER BY CREATED_AT DESC, AUDIT_ID DESC) WHERE ROWNUM <= " + safeLimit);
    }

    /** Lấy audit theo record, dùng khi cần xem lịch sử một lô/QR cụ thể. */
    public List<Map<String, Object>> byRecord(String recordId) {
        return jdbc.queryForList("SELECT * FROM AUDIT_LOGS WHERE RECORD_ID=? ORDER BY CREATED_AT DESC, AUDIT_ID DESC", recordId);
    }
}
