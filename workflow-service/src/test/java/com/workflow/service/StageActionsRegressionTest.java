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

@SpringBootTest
public class StageActionsRegressionTest {

        @Autowired
        private WorkflowDefinitionService workflowService;

        @Autowired
        private DeploymentService deploymentService;

        @Autowired
        private CaseService caseService;

        /**
         * Test reproduces regression where Rework (Action -> Specific Stage)
         * and Reject (Action -> End) were failing to route correctly.
         */
        @Test
        @Transactional
        public void testReworkAndRejectActions() {
                // 1. Define Workflow
                String wfCode = "REGRESSION_TEST_ACTIONS";
                WorkflowMaster wf = new WorkflowMaster();
                wf.setWorkflowName("Regression Test Actions");
                wf.setWorkflowCode(wfCode);
                wf.setAssociatedModule("Test");
                wf.setSlaDurationDays(new BigDecimal("1.0"));
                workflowService.saveWorkflow(wf, "test-user");

                // 2. Define Stage 1: "Input"
                StageConfig stage1 = new StageConfig();
                stage1.setWorkflowCode(wfCode);
                stage1.setStageCode("S1");
                stage1.setStageName("Input Stage");
                stage1.setSequenceOrder(1);
                stage1.setNestedWorkflow(false);
                // Action: Submit -> Next (S2)
                com.workflow.service.entity.StageAction a1 = new com.workflow.service.entity.StageAction();
                a1.setActionLabel("SUBMIT");
                a1.setTargetType("NEXT");
                a1.setStageConfig(stage1);
                stage1.getActions().add(a1);
                workflowService.saveStage(stage1, "test-user");

                // 3. Define Stage 2: "Review"
                StageConfig stage2 = new StageConfig();
                stage2.setWorkflowCode(wfCode);
                stage2.setStageCode("S2");
                stage2.setStageName("Review Stage");
                stage2.setSequenceOrder(2);
                stage2.setNestedWorkflow(false);

                // Action: Approve -> Next (End of workflow in this case)
                com.workflow.service.entity.StageAction a2_approve = new com.workflow.service.entity.StageAction();
                a2_approve.setActionLabel("APPROVE");
                a2_approve.setTargetType("NEXT");
                a2_approve.setStageConfig(stage2);

                // Action: Rework -> Specific (S1)
                com.workflow.service.entity.StageAction a2_rework = new com.workflow.service.entity.StageAction();
                a2_rework.setActionLabel("REWORK");
                a2_rework.setTargetType("SPECIFIC");
                a2_rework.setTargetStage("S1");
                a2_rework.setStageConfig(stage2);

                // Action: Reject -> End
                com.workflow.service.entity.StageAction a2_reject = new com.workflow.service.entity.StageAction();
                a2_reject.setActionLabel("REJECT");
                a2_reject.setTargetType("END");
                a2_reject.setStageConfig(stage2);

                stage2.getActions().add(a2_approve);
                stage2.getActions().add(a2_rework);
                stage2.getActions().add(a2_reject);
                workflowService.saveStage(stage2, "test-user");

                // 4. Deploy
                deploymentService.deployWorkflow(wfCode);

                // 5. Start Case
                String caseId = caseService.initiateCase(wfCode, null, "test-user");

                // --- SCENARIO 1: Test Rework (S1 -> S2 -> S1) ---

                // Complete S1 (Submit)
                StageDTO s1Task = caseService.getStages(caseId).stream()
                                .filter(s -> "ACTIVE".equals(s.getStatus()) && "S1".equals(s.getStageCode()))
                                .findFirst().orElseThrow(() -> new AssertionError("S1 should be active"));

                Map<String, Object> vars = new HashMap<>();
                vars.put("outcome", "SUBMIT");
                caseService.completeTask(s1Task.getTaskId(), vars, "test-user");

                // Should be at S2
                StageDTO s2Task = caseService.getStages(caseId).stream()
                                .filter(s -> "ACTIVE".equals(s.getStatus()) && "S2".equals(s.getStageCode()))
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("S2 should be active after S1 submit"));

                // Complete S2 (Rework)
                vars.put("outcome", "REWORK");
                caseService.completeTask(s2Task.getTaskId(), vars, "test-user");

                // Should be back at S1
                List<StageDTO> stages = caseService.getStages(caseId);
                StageDTO s1TaskBack = stages.stream()
                                .filter(s -> "ACTIVE".equals(s.getStatus()))
                                .findFirst().orElse(null);

                assertThat(s1TaskBack).isNotNull();
                assertThat(s1TaskBack.getStageCode()).as("After Rework, should be back at S1").isEqualTo("S1");

                // --- SCENARIO 2: Test Reject (S1 -> S2 -> End) ---
                // (Fast forward S1 -> S2)
                vars.put("outcome", "SUBMIT");
                caseService.completeTask(s1TaskBack.getTaskId(), vars, "test-user");

                StageDTO s2TaskAgain = caseService.getStages(caseId).stream()
                                .filter(s -> "ACTIVE".equals(s.getStatus()) && "S2".equals(s.getStageCode()))
                                .findFirst().orElseThrow(() -> new AssertionError("S2 should be active again"));

                // Complete S2 (Reject)
                vars.put("outcome", "REJECT");
                caseService.completeTask(s2TaskAgain.getTaskId(), vars, "test-user");

                // Should be ENDED
                com.workflow.service.dto.CaseDTO caseDetails = caseService.getCaseDetails(caseId);
                assertThat(caseDetails.getStatus()).as("After Reject, case should be ENDED").isEqualTo("ENDED");
        }
}
