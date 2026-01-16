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

    // K. Stage Actions (Refactored to separate table)
    @OneToMany(mappedBy = "stageConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private java.util.List<StageAction> actions = new java.util.ArrayList<>();

    // Migration Support
    @Column(name = "allowed_actions_legacy", columnDefinition = "TEXT")
    private String allowedActionsLegacy;

    // T. Parallel Stages
    private String parallelGrouping;

    // V. Rule Stage Integration
    @com.fasterxml.jackson.annotation.JsonProperty("isRuleStage")
    private Boolean isRuleStage = false;

    private String ruleKey;

    // Z. Advanced Routing
    @Column(columnDefinition = "TEXT")
    private String entryCondition; // Expression to evaluate before entering stage

    @Column(columnDefinition = "TEXT")
    private String routingRules; // JSON Structure for branching logic

    // Y.5 Rework Configuration (Parent-side)
    @Column(columnDefinition = "TEXT")
    private String exceptionRules; // JSON List of {errorCode, targetStageCode}

    // AF. Simplified Assignment Configuration
    @Column(columnDefinition = "TEXT")
    private String assignmentRules; // JSON Structure for Assignment Logic

    public boolean isRuleStage() {
        return Boolean.TRUE.equals(this.isRuleStage);
    }
}
