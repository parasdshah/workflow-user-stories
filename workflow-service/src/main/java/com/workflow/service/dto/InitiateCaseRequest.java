package com.workflow.service.dto;

import lombok.Data;
import java.util.Map;

@Data
public class InitiateCaseRequest {
    private String workflowCode;
    private Map<String, Object> variables;
    private String userId;
}
