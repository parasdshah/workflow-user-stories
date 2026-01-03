package com.workflow.service.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class DeploymentHistoryDTO {
    private String id;
    private String name; // Workflow Code usually
    private Date deploymentTime;
    private String status; // ACTIVE, SUSPENDED
}
