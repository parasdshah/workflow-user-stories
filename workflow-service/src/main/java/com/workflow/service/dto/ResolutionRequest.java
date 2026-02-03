package com.workflow.service.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ResolutionRequest {
    private String role;
    private String region;
    private String product;
    private String businessSegment;
    private String businessSubSegment;
    private BigDecimal amount;
    private Map<String, Object> context;
}
