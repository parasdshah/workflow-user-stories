package com.workflow.service.dto;

import lombok.Data;

@Data
public class DecisionTableDTO {
    private String id;
    private String key;
    private String name;
    private int version;
    private String deploymentId;
    private String resourceName;
    private String category;
}
