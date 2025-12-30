package com.workflow.service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CaseDTO {
    private String caseId; // Process Instance ID
    private String workflowCode;
    private String workflowName;
    private String status; // ACTIVE, ENDED (Not Enum to avoid serialization issues if extended)
    
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    private String startUserId;
    private String parentCaseId;
    
    private java.util.Map<String, Object> processVariables;
}
