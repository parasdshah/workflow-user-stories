package com.workflow.service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "audit_trail")
public class AuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityName;
    private String entityId;
    private String action;
    private String changedBy;
    private LocalDateTime changedAt;

    @Lob
    private String changes; // JSON representation of changes

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
