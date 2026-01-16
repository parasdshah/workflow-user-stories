package com.workflow.hrms.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ResolutionRequest {
    private String role;           // e.g. "CREDIT_APPROVER"
    private String region;         // e.g. "Mumbai" (Name)
    private String product;        // e.g. "Home Loan"
    private BigDecimal amount;     // e.g. 50000
    private Map<String, Object> context; // Extra arbitrary context
}
