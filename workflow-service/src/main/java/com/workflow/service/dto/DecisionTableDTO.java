package com.workflow.service.dto;

import lombok.Data;

import java.util.Date;

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
