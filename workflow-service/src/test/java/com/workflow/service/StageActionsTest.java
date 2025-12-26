package com.workflow.service;

import com.workflow.service.dto.StageDTO;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
import com.workflow.service.service.CaseService;
import com.workflow.service.service.DeploymentService;
import com.workflow.service.service.WorkflowDefinitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
public class StageActionsTest {

    @Autowired
    private WorkflowDefinitionService workflowService;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private CaseService caseService;

    @Test
    @Transactional
    public void testStageActionsFlow() {
        // 1. Define Workflow
        WorkflowMaster wf = new WorkflowMaster();
        wf.setWorkflowName("Action Test Flow");
        wf.setWorkflowCode("ACTION_TEST_001");
        wf.setAssociatedModule("Test");
        wf.setSlaDurationDays(new BigDecimal("1.0"));
        workflowService.saveWorkflow(wf, "test-user");

        // 2. Define Stage with Allowed Actions
        StageConfig stage1 = new StageConfig();
        stage1.setWorkflowCode("ACTION_TEST_001");
        stage1.setStageCode("STAGE_1");
        stage1.setStageName("Initial Review");
        stage1.setSequenceOrder(1);
        stage1.setNestedWorkflow(false);
        stage1.setAllowedActions("[\"APPROVE\",\"REJECT\"]");
        workflowService.saveStage(stage1, "test-user");

        // 3. Generate & Deploy
        deploymentService.deployWorkflow("ACTION_TEST_001");

        // 4. Start Case
        String caseId = caseService.initiateCase("ACTION_TEST_001", null, "test-user");
        assertThat(caseId).isNotNull();

        // 5. Verify Stage Actions in Case Details
        List<StageDTO> stages = caseService.getStages(caseId);
        assertThat(stages).isNotEmpty();
        StageDTO activeStage = stages.stream().filter(s -> "ACTIVE".equals(s.getStatus())).findFirst().orElse(null);
        assertThat(activeStage).isNotNull();
        assertThat(activeStage.getAllowedActions()).contains("APPROVE", "REJECT");

        // 6. Test Invalid Outcome
        Map<String, Object> invalidVariables = new HashMap<>();
        invalidVariables.put("outcome", "INVALID_ACTION");
        Throwable thrown = catchThrowable(
                () -> caseService.completeTask(activeStage.getTaskId(), invalidVariables, "test-user"));
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid outcome");

        // 7. Test Valid Outcome
        Map<String, Object> validVariables = new HashMap<>();
        validVariables.put("outcome", "APPROVE");
        caseService.completeTask(activeStage.getTaskId(), validVariables, "test-user");

        // Verify task completed (stage status)
        List<StageDTO> updatedStages = caseService.getStages(caseId);
        StageDTO completedStage = updatedStages.stream()
                .filter(s -> s.getTaskId().equals(activeStage.getTaskId()))
                .findFirst().orElse(null);
        assertThat(completedStage).isNotNull();
        assertThat(completedStage.getStatus()).isEqualTo("COMPLETED");
    }
}
