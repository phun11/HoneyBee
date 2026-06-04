package com.example.agritrace.service;

import com.example.agritrace.dto.OfflineActionRequest;
import com.example.agritrace.dto.OfflineSyncRequest;
import com.example.agritrace.dto.OfflineSyncResult;
import com.example.agritrace.dto.TransportRequest;
import com.example.agritrace.repository.AuditRepository;
import com.example.agritrace.repository.OfflineSyncRepository;
import com.example.agritrace.repository.SystemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OfflineSyncService {
    private final OfflineSyncRepository syncRepo;
    private final SystemRepository systemRepo;
    private final AuditRepository audit;
    private final ObjectMapper mapper;

    public OfflineSyncService(OfflineSyncRepository syncRepo, SystemRepository systemRepo, AuditRepository audit, ObjectMapper mapper) {
        this.syncRepo = syncRepo;
        this.systemRepo = systemRepo;
        this.audit = audit;
        this.mapper = mapper;
    }

    public Map<String, Object> syncBatch(OfflineSyncRequest request) {
        List<OfflineSyncResult> results = new ArrayList<>();
        if (request == null || request.actions == null || request.actions.isEmpty()) {
            return Map.of("total", 0, "success", 0, "failed", 0, "duplicate", 0, "results", results);
        }
        int success = 0, failed = 0, duplicate = 0;
        for (OfflineActionRequest action : request.actions) {
            OfflineSyncResult r = syncOne(request, action);
            results.add(r);
            if ("SUCCESS".equals(r.status)) success++;
            else if ("DUPLICATE".equals(r.status)) duplicate++;
            else failed++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", request.actions.size());
        out.put("success", success);
        out.put("failed", failed);
        out.put("duplicate", duplicate);
        out.put("results", results);
        return out;
    }

    @Transactional
    public OfflineSyncResult syncOne(OfflineSyncRequest batch, OfflineActionRequest action) {
        String clientActionId = safe(action.clientActionId);
        if (clientActionId == null) {
            return new OfflineSyncResult(null, action.actionType, "FAILED", "Missing clientActionId", action.entityId, null);
        }
        try {
            if (syncRepo.exists(clientActionId)) {
                String status = syncRepo.status(clientActionId);
                return new OfflineSyncResult(clientActionId, action.actionType, "DUPLICATE", "Action already received on server: " + status, action.entityId, null);
            }
            String payloadJson = mapper.writeValueAsString(action.payload == null ? Map.of() : action.payload);
            syncRepo.insertPending(batch.deviceId, firstNonBlank(action.username, batch.username), firstNonBlank(action.roleName, batch.roleName), action, payloadJson);
            handleBusinessAction(action);
            String entityId = action.entityId;
            if ((entityId == null || entityId.isBlank()) && action.payload != null && action.payload.get("productId") != null) {
                entityId = String.valueOf(action.payload.get("productId"));
            }
            syncRepo.markSuccess(clientActionId, entityId, "Synced successfully");
            audit.logOfflineSync("OFFLINE_SYNC_LOGS", entityId, "SYNC_OFFLINE", null, payloadJson,
                    firstNonBlank(action.username, batch.username, "offline-user"), clientActionId, batch.deviceId, action.offlineCreatedAt);
            Long id = null;
            try { id = entityId == null ? null : Long.valueOf(entityId); } catch (Exception ignored) {}
            return new OfflineSyncResult(clientActionId, action.actionType, "SUCCESS", "Synced successfully", entityId, id);
        } catch (Exception e) {
            try {
                if (!syncRepo.exists(clientActionId)) {
                    String payloadJson = mapper.writeValueAsString(action.payload == null ? Map.of() : action.payload);
                    syncRepo.insertPending(batch.deviceId, firstNonBlank(action.username, batch.username), firstNonBlank(action.roleName, batch.roleName), action, payloadJson);
                }
                syncRepo.markFailed(clientActionId, rootMessage(e));
            } catch (Exception ignored) {}
            return new OfflineSyncResult(clientActionId, action.actionType, "FAILED", rootMessage(e), action.entityId, null);
        }
    }

    private void handleBusinessAction(OfflineActionRequest action) {
        String type = safe(action.actionType);
        if (type == null) throw new IllegalArgumentException("Missing actionType");
        switch (type) {
            case "TRANSPORT_PICKUP" -> systemRepo.pickupShipment(toTransportRequest(action));
            case "TRANSPORT_UPDATE_STEP" -> systemRepo.addTransport(toTransportRequest(action));
            case "TRANSPORT_MARK_DELIVERED" -> systemRepo.deliverShipment(toTransportRequest(action));
            case "TRANSPORT_REPORT_ISSUE" -> systemRepo.reportIssue(toTransportRequest(action));
            default -> throw new IllegalArgumentException("Unsupported offline actionType: " + type + ". Demo offline hiện hỗ trợ các thao tác Transport trước.");
        }
    }

    private TransportRequest toTransportRequest(OfflineActionRequest a) {
        Map<String, Object> p = a.payload == null ? Map.of() : a.payload;
        TransportRequest r = new TransportRequest();
        r.productId = longValue(firstValue(p, "productId", "PRODUCT_ID"));
        if (r.productId == null && a.entityId != null && !a.entityId.isBlank()) r.productId = Long.valueOf(a.entityId);
        r.transporterId = longValue(firstValue(p, "transporterId", "TRANSPORTER_ID"));
        r.transportCompany = str(firstValue(p, "transportCompany", "TRANSPORT_COMPANY"));
        r.fromLocation = str(firstValue(p, "fromLocation", "FROM_LOCATION"));
        r.toLocation = str(firstValue(p, "toLocation", "TO_LOCATION"));
        r.currentLocation = str(firstValue(p, "currentLocation", "CURRENT_LOCATION"));
        r.storageTemperature = decimalValue(firstValue(p, "storageTemperature", "STORAGE_TEMPERATURE"));
        r.humidity = decimalValue(firstValue(p, "humidity", "HUMIDITY"));
        r.sealStatus = str(firstValue(p, "sealStatus", "SEAL_STATUS"));
        r.status = str(firstValue(p, "status", "STATUS"));
        r.note = str(firstValue(p, "note", "NOTE"));
        r.issueNote = str(firstValue(p, "issueNote", "ISSUE_NOTE"));
        r.userId = longValue(firstValue(p, "userId", "USER_ID"));
        return r;
    }

    public List<Map<String, Object>> latestLogs(int limit) { return syncRepo.latest(limit); }

    private Object firstValue(Map<String, Object> p, String... keys) {
        for (String k : keys) if (p.containsKey(k)) return p.get(k);
        return null;
    }
    private String safe(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private String str(Object o) { return o == null ? null : String.valueOf(o); }
    private Long longValue(Object o) {
        if (o == null || String.valueOf(o).isBlank()) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(String.valueOf(o));
    }
    private BigDecimal decimalValue(Object o) {
        if (o == null || String.valueOf(o).isBlank()) return null;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(o));
    }
    private String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }
    private String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? e.getClass().getSimpleName() : t.getMessage();
    }
}
