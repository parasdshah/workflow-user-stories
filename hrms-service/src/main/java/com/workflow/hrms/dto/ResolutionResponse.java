package com.workflow.hrms.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ResolutionResponse {
    private List<String> userIds;
    private Map<String, String> metadata; // e.g. "reason": "Matched Mumbai Branch Manager"

    public ResolutionResponse(List<String> userIds, String reason) {
        this.userIds = userIds;
        this.metadata = Map.of("reason", reason);
    }
}
