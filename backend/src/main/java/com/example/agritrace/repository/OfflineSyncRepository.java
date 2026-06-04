package com.example.agritrace.repository;

import com.example.agritrace.dto.OfflineActionRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class OfflineSyncRepository {
    private final JdbcTemplate jdbc;

    public OfflineSyncRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(String clientActionId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM OFFLINE_SYNC_LOGS WHERE CLIENT_ACTION_ID=?", Long.class, clientActionId);
        return count != null && count > 0;
    }

    public String status(String clientActionId) {
        return jdbc.queryForObject("SELECT RESULT_STATUS FROM OFFLINE_SYNC_LOGS WHERE CLIENT_ACTION_ID=?", String.class, clientActionId);
    }

    public void insertPending(String deviceId, String username, String roleName, OfflineActionRequest a, String payloadJson) {
        jdbc.update("""
                INSERT INTO OFFLINE_SYNC_LOGS(CLIENT_ACTION_ID, DEVICE_ID, USERNAME, ROLE_NAME, ACTION_TYPE, ENTITY_TYPE, ENTITY_ID,
                    REQUEST_PAYLOAD, RESULT_STATUS, OFFLINE_CREATED_AT)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """, a.clientActionId, deviceId, username, roleName, a.actionType, a.entityType, a.entityId,
                payloadJson, "PENDING", ts(a.offlineCreatedAt));
    }

    public void markSuccess(String clientActionId, String entityId, String message) {
        jdbc.update("""
                UPDATE OFFLINE_SYNC_LOGS
                SET RESULT_STATUS='SUCCESS', ENTITY_ID=COALESCE(?, ENTITY_ID), ERROR_MESSAGE=?, SYNCED_AT=CURRENT_TIMESTAMP
                WHERE CLIENT_ACTION_ID=?
                """, entityId, message, clientActionId);
    }

    public void markFailed(String clientActionId, String message) {
        jdbc.update("""
                UPDATE OFFLINE_SYNC_LOGS
                SET RESULT_STATUS='FAILED', ERROR_MESSAGE=?, SYNCED_AT=CURRENT_TIMESTAMP
                WHERE CLIENT_ACTION_ID=?
                """, message, clientActionId);
    }

    public List<Map<String, Object>> latest(int limit) {
        int safe = Math.max(1, Math.min(limit, 100));
        return jdbc.queryForList("SELECT * FROM (SELECT * FROM OFFLINE_SYNC_LOGS ORDER BY CREATED_AT DESC, SYNC_ID DESC) WHERE ROWNUM <= " + safe);
    }

    private Timestamp ts(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
