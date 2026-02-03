package com.workflow.service.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupWorkloadDTO {
    private String groupId;
    private String groupName;
    private int pendingCount;
    private List<UserWorkloadDTO.TaskSummaryDTO> tasks; // Reuse TaskSummaryDTO
}
