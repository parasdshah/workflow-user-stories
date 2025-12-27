package com.workflow.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStatsDTO {
    private String workflowCode;
    private String workflowName;
    private String associatedModule; // New
    private String status; // ACTIVE, DELETED
    private long activeInstances;
    private long completedInstances;
}
