package com.example.agritrace.service;

import com.example.agritrace.repository.TraceRepository;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * TraceService là lớp trung gian cho luồng truy xuất nguồn gốc.
 *
 * Vì hệ thống đang dùng JDBC Template và tập trung DBMS, service này giữ mỏng:
 * - Controller nhận request từ web/mobile.
 * - Service điều phối nghiệp vụ truy xuất.
 * - Repository thực hiện transaction, ghi scan log, tạo alert và đọc dữ liệu DB.
 */
@Service
public class TraceService {
    private final TraceRepository repo;

    public TraceService(TraceRepository repo) {
        this.repo = repo;
    }

    /** Truy xuất theo productId, dùng cho trang demo/admin nội bộ. */
    public Map<String, Object> trace(Long productId) {
        return repo.trace(productId);
    }

    /** Resolve QR_TOKEN thành PRODUCT_ID, dùng khi QR chỉ chứa token ngắn. */
    public Long resolveToken(String token) {
        return repo.productIdByToken(token);
    }

    /**
     * Truy xuất public theo QR token/signature.
     *
     * Hàm này là lõi của trải nghiệm khách hàng:
     * - Xác minh token/signature.
     * - Ghi QR scan log.
     * - Phát hiện cảnh báo QR bất thường.
     * - Trả dữ liệu đã gom sẵn cho UI mobile-first.
     */
    public Map<String, Object> traceByToken(String token, String sig, String ip, String ua) {
        return repo.traceByToken(token, sig, ip, ua);
    }
}
