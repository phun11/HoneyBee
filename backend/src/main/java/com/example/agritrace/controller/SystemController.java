package com.example.agritrace.controller;

import com.example.agritrace.dto.ApiResponse;
import com.example.agritrace.dto.CertificateRequest;
import com.example.agritrace.dto.StoreReceiveRequest;
import com.example.agritrace.dto.StoreSaleRequest;
import com.example.agritrace.dto.TransportRequest;
import com.example.agritrace.dto.ProductRequest;
import com.example.agritrace.service.ProductService;
import com.example.agritrace.service.TraceService;
import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import com.example.agritrace.repository.AuditRepository;
import com.example.agritrace.repository.SystemRepository;
import org.springframework.web.bind.annotation.*;

/** API vận hành hệ thống cho các dashboard role. */
@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final SystemRepository repo;
    private final AuditRepository audit;
    private final ProductService productService;
    private final TraceService traceService;

    public SystemController(SystemRepository repo, AuditRepository audit, ProductService productService, TraceService traceService) {
        this.repo = repo;
        this.audit = audit;
        this.productService = productService;
        this.traceService = traceService;
    }

    @GetMapping("/dashboard") public ApiResponse<?> dashboard() { return ApiResponse.ok(repo.dashboard()); }
    @GetMapping("/health") public ApiResponse<?> health() { return ApiResponse.ok(repo.health()); }
    @GetMapping("/users") public ApiResponse<?> users() { return ApiResponse.ok(repo.users()); }
    @GetMapping("/transporters") public ApiResponse<?> transporters() { return ApiResponse.ok(repo.transporters()); }
    @GetMapping("/stores") public ApiResponse<?> stores() { return ApiResponse.ok(repo.stores()); }
    @GetMapping("/security-alerts") public ApiResponse<?> alerts() { return ApiResponse.ok(repo.alerts()); }
    @GetMapping("/qr-codes") public ApiResponse<?> qrCodes() { return ApiResponse.ok(repo.qrCodes()); }
    @GetMapping("/audit-logs") public ApiResponse<?> auditLogs() { return ApiResponse.ok(audit.latest(80)); }


    /**
     * Prewarm cache cho PWA/offline QR scan.
     * Không ghi QR_SCAN_LOGS vì đây chỉ là tải dữ liệu cache nội bộ, không phải khách hàng quét thật.
     */
    @GetMapping("/offline-trace-cache")
    public ApiResponse<?> offlineTraceCache() {
        List<Map<String, Object>> qrs = repo.qrCodes();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> qr : qrs) {
            Object pidObj = qr.get("PRODUCT_ID");
            if (pidObj == null) continue;
            Long productId = Long.valueOf(String.valueOf(pidObj));
            rows.add(Map.of(
                    "productId", productId,
                    "qrToken", String.valueOf(qr.get("QR_TOKEN")),
                    "qrSignature", String.valueOf(qr.get("QR_SIGNATURE")),
                    "trace", traceService.trace(productId)
            ));
        }
        return ApiResponse.ok(rows);
    }

    @GetMapping("/certificates")
    public ApiResponse<?> certificates(@RequestParam(value = "productId", required = false) Long productId) {
        return ApiResponse.ok(repo.certificates(productId));
    }

    @PostMapping("/certificates")
    public ApiResponse<?> addCertificate(@RequestBody CertificateRequest request) {
        repo.addCertificate(request);
        return ApiResponse.ok("Certificate created");
    }

    @PostMapping("/transport")
    public ApiResponse<?> addTransport(@RequestBody TransportRequest request) {
        repo.addTransport(request);
        return ApiResponse.ok("Transport updated");
    }

    // ================= FARM ROLE =================
    // Farmer là bên tạo lô hàng mới và chuyển lô sang trạng thái chờ vận chuyển.
    @GetMapping("/farm/products")
    public ApiResponse<?> farmProducts(@RequestParam(value = "farmId", required = false) Long farmId) {
        return ApiResponse.ok(productService.findByFarm(farmId));
    }

    @PostMapping("/farm/products")
    public ApiResponse<?> createFarmProduct(@RequestBody ProductRequest request) {
        Long id = productService.create(request);
        return ApiResponse.ok(Map.of("productId", id, "message", "Farm created new batch"));
    }

    @PostMapping("/farm/products/{id}/ready-for-transport")
    public ApiResponse<?> readyForTransport(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId")));
        String note = body.get("note") == null ? null : String.valueOf(body.get("note"));
        productService.markReadyForTransport(id, userId, note);
        return ApiResponse.ok("Batch is ready for transport");
    }

    // ================= TRANSPORT ROLE =================
    // Transporter chỉ xử lý lô chờ vận chuyển/đang vận chuyển và xem lịch sử vận chuyển.
    @GetMapping("/transport/shipments/pending")
    public ApiResponse<?> pendingShipments(@RequestParam(value = "transporterId", required = false) Long transporterId) {
        return ApiResponse.ok(repo.pendingShipments(transporterId));
    }

    @GetMapping("/transport/shipments/history")
    public ApiResponse<?> transportHistory(@RequestParam(value = "transporterId", required = false) Long transporterId) {
        return ApiResponse.ok(repo.transportHistory(transporterId));
    }

    @PostMapping("/transport/shipments/{id}/pickup")
    public ApiResponse<?> pickupShipment(@PathVariable("id") Long id, @RequestBody TransportRequest request) {
        request.productId = id;
        repo.pickupShipment(request);
        return ApiResponse.ok("Shipment picked up and marked IN_TRANSIT");
    }

    @PostMapping("/transport/shipments/{id}/route")
    public ApiResponse<?> updateRoute(@PathVariable("id") Long id, @RequestBody TransportRequest request) {
        request.productId = id;
        repo.addTransport(request);
        return ApiResponse.ok("Transport route step inserted");
    }

    @PostMapping("/transport/shipments/{id}/deliver")
    public ApiResponse<?> deliverShipment(@PathVariable("id") Long id, @RequestBody TransportRequest request) {
        request.productId = id;
        repo.deliverShipment(request);
        return ApiResponse.ok("Shipment delivered to store");
    }

    @PostMapping("/transport/shipments/{id}/issue")
    public ApiResponse<?> reportIssue(@PathVariable("id") Long id, @RequestBody TransportRequest request) {
        request.productId = id;
        repo.reportIssue(request);
        return ApiResponse.ok("Transport issue reported");
    }

    @GetMapping("/transport/shipments/{id}/history")
    public ApiResponse<?> shipmentHistory(@PathVariable("id") Long id) {
        return ApiResponse.ok(repo.transportHistory(null).stream().filter(x -> String.valueOf(x.get("PRODUCT_ID")).equals(String.valueOf(id))).toList());
    }


    @PostMapping("/store/receive")
    public ApiResponse<?> receive(@RequestBody StoreReceiveRequest request) {
        repo.confirmStoreReceive(request);
        return ApiResponse.ok("Store receive confirmed");
    }

    @GetMapping("/store/products")
    public ApiResponse<?> storeProducts(@RequestParam(value = "storeId", required = false) Long storeId) {
        return ApiResponse.ok(repo.storeProducts(storeId));
    }

    @PostMapping("/store/reject")
    public ApiResponse<?> reject(@RequestBody StoreReceiveRequest request) {
        request.status = "REJECTED";
        repo.confirmStoreReceive(request);
        return ApiResponse.ok("Store rejection/return request recorded");
    }

    @PostMapping("/store/sale-status")
    public ApiResponse<?> markSaleStatus(@RequestBody StoreSaleRequest request) {
        repo.markQrSaleStatus(request);
        return ApiResponse.ok("QR sale status updated");
    }
}
