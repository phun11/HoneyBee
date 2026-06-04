package com.example.agritrace.dto;

import java.util.ArrayList;
import java.util.List;

public class OfflineSyncRequest {
    public String deviceId;
    public String username;
    public String roleName;
    public List<OfflineActionRequest> actions = new ArrayList<>();
}
