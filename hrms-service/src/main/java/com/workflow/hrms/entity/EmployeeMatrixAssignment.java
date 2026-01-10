package com.workflow.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "employee_matrix_assignment")
public class EmployeeMatrixAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeMaster employee;

    @ManyToOne
    @JoinColumn(name = "role_code", nullable = false)
    private RoleMaster role;

    @ManyToOne
    @JoinColumn(name = "scope_region_id", nullable = false)
    private RefRegion scopeRegion;

    @ManyToOne
    @JoinColumn(name = "scope_segment_id")
    private RefBusinessSegment scopeSegment; // Nullable (All Segments)

    @ManyToOne
    @JoinColumn(name = "scope_product_id")
    private RefProduct scopeProduct; // Nullable (All Products)

    @ManyToOne
    @JoinColumn(name = "reporting_manager_id")
    private EmployeeMaster reportingManager;

    // Authority Limits
    private BigDecimal approvalLimit;
    private String currencyCode; // USD, INR
    private String denomination; // ACTUALS, LAKHS, MILLIONS
}
