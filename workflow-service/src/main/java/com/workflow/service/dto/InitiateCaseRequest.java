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

    private Map<String, Object> otherFields = new java.util.HashMap<>();

    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void add(String key, Object value) {
        otherFields.put(key, value);
    }

    public Map<String, Object> getEffectiveVariables() {
        Map<String, Object> effective = new java.util.HashMap<>();
        if (otherFields != null) {
            effective.putAll(otherFields);
        }
        if (variables != null) {
            effective.putAll(variables);
        }
        return effective;
    }
}
