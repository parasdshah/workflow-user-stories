package com.workflow.hrms.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserAttributes {
    private String userId;
    private String fullName;
    private String email;
    private String role;
    private BigDecimal approvalLimit;
    private String currency;
}
