package com.workflow.service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "workflow_master")
public class WorkflowMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String workflowName;

    @Column(nullable = false, unique = true)
    private String workflowCode;

    private String completionApiEndpoint;

    private String associatedModule;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, DELETED

    // SLA in days (e.g. 0.5, 1.0)
    private BigDecimal slaDurationDays;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
