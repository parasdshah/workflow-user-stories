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
public class UserStoryboardDTO {
    private String userId;
    private String userName;
    private List<TaskSummaryDTO> newTasks;
    private List<TaskSummaryDTO> wipTasks;
    private List<TaskSummaryDTO> closedTasks;

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
        private LocalDateTime endTime; // For closed tasks
        private String workflowName;
        private String status; // For WIP tasks
    }
}
