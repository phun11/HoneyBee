package com.example.agritrace.dto;

import java.math.BigDecimal;

public class StoreReceiveRequest {
    public Long productId;
    public Long storeId;
    public Long userId;
    public BigDecimal quantityReceived;
    public String status;
    public String note;
    public String rejectReason;
}
