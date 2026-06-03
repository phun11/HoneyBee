package com.example.agritrace.dto;

import java.time.LocalDate;

public class CertificateRequest {
    public Long farmId;
    public Long productId;
    public String certificateName;
    public String issuedBy;
    public LocalDate issueDate;
    public LocalDate expiredDate;
    public String fileUrl;
    public String status;
}
