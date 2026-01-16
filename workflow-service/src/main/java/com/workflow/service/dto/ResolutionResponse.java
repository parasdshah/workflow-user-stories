package com.workflow.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ResolutionResponse {
    private List<String> userIds;
    private Map<String, String> metadata;
}
