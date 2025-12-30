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
    
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdTime;
    
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate; // For SLA
    private String allowedActions; // e.g. ["APPROVE", "REJECT"]
    private String actionTaken; // e.g. "APPROVE"
    private String subProcessInstanceId; // For Call Activity
    private String parentCaseId; // To group child cases under parent
    private String parentWorkflowCode;
    private String parentWorkflowName;
    
    private java.util.Map<String, Object> processVariables;
}
