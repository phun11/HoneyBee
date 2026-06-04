package com.example.agritrace.controller;

import com.example.agritrace.dto.ApiResponse;
import com.example.agritrace.dto.OfflineSyncRequest;
import com.example.agritrace.service.OfflineSyncService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/offline-sync")
public class OfflineSyncController {
    private final OfflineSyncService service;

    public OfflineSyncController(OfflineSyncService service) {
        this.service = service;
    }

    @PostMapping("/batch")
    public ApiResponse<?> syncBatch(@RequestBody OfflineSyncRequest request) {
        return ApiResponse.ok(service.syncBatch(request));
    }

    @GetMapping("/logs")
    public ApiResponse<?> logs(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ApiResponse.ok(service.latestLogs(limit));
    }
}
