package com.workflow.service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CaseDTO {
    private String caseId; // Process Instance ID
    private String workflowCode;
    private String workflowName;
    private String status; // ACTIVE, ENDED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String startUserId;
}
