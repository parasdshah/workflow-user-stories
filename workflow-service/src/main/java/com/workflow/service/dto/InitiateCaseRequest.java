package com.workflow.service.dto;

import lombok.Data;
import java.util.Map;

@Data
public class InitiateCaseRequest {
    private String workflowCode;
    private Map<String, Object> variables;
    private String userId;

    public String getWorkflowCode() {
        return workflowCode;
    }

    public void setWorkflowCode(String workflowCode) {
        this.workflowCode = workflowCode;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
