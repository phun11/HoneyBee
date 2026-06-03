package com.example.agritrace.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
    public Long productId;
    public Long farmId;
    public String batchCode;
    public String productName;
    public String category;
    public String description;
    public BigDecimal price;
    public BigDecimal quantity;
    public String unit;
    public String status;
    public String imageUrl;
    public String productImageBase64;
    public String productImageMime;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
