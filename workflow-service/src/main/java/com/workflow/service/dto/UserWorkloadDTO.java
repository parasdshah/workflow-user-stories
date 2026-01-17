package com.workflow.service.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWorkloadDTO {
    private String userId;
    private String userName;
    private int pendingCount;
    private List<TaskSummaryDTO> tasks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSummaryDTO {
        private String taskId;
        private String caseId;
        private String stageName;
        private String stageCode;
        private LocalDateTime createdTime;
        private LocalDateTime dueDate;
        private String workflowName;
    }
}
