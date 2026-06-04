package com.example.agritrace.dto;

public class OfflineSyncResult {
    public String clientActionId;
    public String actionType;
    public String status;
    public String message;
    public String entityId;
    public Long serverRecordId;

    public OfflineSyncResult() {}
    public OfflineSyncResult(String clientActionId, String actionType, String status, String message, String entityId, Long serverRecordId) {
        this.clientActionId = clientActionId;
        this.actionType = actionType;
        this.status = status;
        this.message = message;
        this.entityId = entityId;
        this.serverRecordId = serverRecordId;
    }
}
