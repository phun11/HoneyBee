package com.example.agritrace.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class OfflineActionRequest {
    public String clientActionId;
    public String actionType;
    public String entityType;
    public String entityId;
    public String username;
    public String roleName;
    public LocalDateTime offlineCreatedAt;
    public Map<String, Object> payload;
}
