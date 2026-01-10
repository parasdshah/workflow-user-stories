package com.workflow.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "role_master")
public class RoleMaster {
    @Id
    private String roleCode; // e.g. "CREDIT_MGR"

    private String roleName;

    private BigDecimal baseAuthorityLimit;
    private String baseCurrency; // e.g. "USD"
}
