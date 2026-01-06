package com.workflow.service.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "stage_actions")
@Data
public class StageAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_config_id")
    @JsonBackReference // Prevent infinite recursion
    @ToString.Exclude
    private StageConfig stageConfig;

    private String actionLabel; // Button Label (APPROVE)
    private String buttonStyle; // primary, danger, etc.
    private String targetType;  // NEXT, SPECIFIC, END
    private String targetStage; // if SPECIFIC
    private String postActionStatus; // e.g. APPROVED, REJECTED

    // Y.5 Rework Configuration
    private String actionType; // COMPLETION (Default), ERROR_TRIGGER
    private String errorCode; // e.g. REWORK_REQUIRED
}
