package com.workflow.service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "stage_config")
public class StageConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String workflowCode;

    @Column(nullable = false)
    private String stageName;

    @Column(nullable = false)
    private String stageCode;

    @Column(nullable = false)
    private Integer sequenceOrder;

    @com.fasterxml.jackson.annotation.JsonProperty("isNestedWorkflow")
    private boolean isNestedWorkflow;

    private String nestedWorkflowCode;

    // Hooks (FQNs)
    private String preEntryHook;
    private String postEntryHook;
    private String preExitHook;
    private String postExitHook;

    // Notification Templates
    private String reminderTemplateId1;
    private String reminderTemplateId2;

    // SLA Configuration (G.5)
    private java.math.BigDecimal slaDurationDays;

}
