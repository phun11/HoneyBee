package com.example.agritrace.dto;

import java.math.BigDecimal;

public class TransportRequest {
    public Long productId;
    public Long transporterId;
    public String transportCompany;
    public String fromLocation;
    public String toLocation;
    public String currentLocation;
    public BigDecimal storageTemperature;
    public BigDecimal humidity;
    public String sealStatus;
    public String status;
    public String note;
    public String issueNote;
    public Long userId;
}
