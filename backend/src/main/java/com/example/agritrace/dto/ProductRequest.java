package com.example.agritrace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Request tạo/cập nhật lô hàng. PRODUCTS là bảng lô hàng trung tâm. */
public class ProductRequest {
    public Long farmId;
    public String batchCode;
    public String productName;
    public String category;
    public String description;
    public BigDecimal price;
    public BigDecimal quantity;
    public String unit;
    public String imageUrl;
    // Ảnh sản phẩm do Farm upload. Frontend gửi base64 phần nội dung, không gồm prefix data:image/...;base64,
    public String productImageBase64;
    public String productImageMime;

    public String cultivationPlace;
    public String farmAddress;
    public LocalDate sowingDate;
    public LocalDate harvestDate;
    public LocalDate expiredDate;
    public String productionProcess;
    public String qualitySummary;
    public String freshnessStatus;

    public Long storeId;
    public Long transporterId;
    public String pickupLocation;
    public String deliveryLocation;
    public BigDecimal requiredTempMin;
    public BigDecimal requiredTempMax;
    public BigDecimal requiredHumidityMin;
    public BigDecimal requiredHumidityMax;
    public String transportNote;
    public String receiverName;
    public String receiverPhone;
    public LocalDateTime expectedDeliveryAt;

    public String certificateName;
    public String certificateIssuer;
    public LocalDate certificateIssueDate;
    public LocalDate certificateExpiredDate;
    public String certificateFileUrl;
}
