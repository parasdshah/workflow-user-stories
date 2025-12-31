package com.workflow.service.dto;

import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExportDto {
    private WorkflowMaster workflow;
    private List<StageConfig> stages;
}
