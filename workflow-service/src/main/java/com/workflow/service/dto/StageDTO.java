package com.workflow.service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StageDTO {
    private String stageName;
    private String workflowCode; // Added for context
    private String stageCode; // User Task ID or Activity ID
    private String taskId;
    private String caseId; // Process Instance ID
    private String status; // ACTIVE, COMPLETED
    private String assignee;
    private LocalDateTime createdTime;
    private LocalDateTime endTime;
    private LocalDateTime dueDate; // For SLA
    private String allowedActions; // e.g. ["APPROVE", "REJECT"]
    private String actionTaken; // e.g. "APPROVE"
    private String subProcessInstanceId; // For Call Activity
}
