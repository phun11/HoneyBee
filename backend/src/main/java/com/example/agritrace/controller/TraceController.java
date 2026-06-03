package com.example.agritrace.controller;

import com.example.agritrace.dto.ApiResponse;
import com.example.agritrace.service.TraceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** Public API cho khách hàng quét QR và API nội bộ xem trace theo productId. */
@RestController
@RequestMapping("/api/trace")
public class TraceController {
    private final TraceService service;
    public TraceController(TraceService service) { this.service = service; }

    /** Truy xuất nội bộ theo productId, dùng cho dashboard/admin. */
    @GetMapping("/product/{id}")
    public ApiResponse<?> trace(@PathVariable("id") Long id) {
        return ApiResponse.ok(service.trace(id));
    }

    /**
     * Public endpoint chính khi quét QR.
     * sig là chữ ký đi kèm URL; nếu thiếu vẫn cho demo nhưng hệ thống sẽ log scan.
     */
    @GetMapping("/token/{token}")
    public ApiResponse<?> byToken(@PathVariable("token") String token,
                                  @RequestParam(value = "sig", required = false) String sig,
                                  HttpServletRequest req) {
        return ApiResponse.ok(service.traceByToken(token, sig, req.getRemoteAddr(), req.getHeader("User-Agent")));
    }

    /** Resolve token thành productId, dùng cho scanner khi cần điều hướng thủ công. */
    @GetMapping("/token/{token}/resolve")
    public ApiResponse<?> resolve(@PathVariable("token") String token) {
        return ApiResponse.ok(service.resolveToken(token));
    }
}
