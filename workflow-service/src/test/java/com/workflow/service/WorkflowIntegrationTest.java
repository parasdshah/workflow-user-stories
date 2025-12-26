package com.workflow.service;

import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
//import com.workflow.service.service.BpmnGeneratorService;
import com.workflow.service.service.DeploymentService;
import com.workflow.service.service.WorkflowDefinitionService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
//import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class WorkflowIntegrationTest {

    @Autowired
    private WorkflowDefinitionService workflowService;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private RuntimeService flowableRuntime;

    @Test
    @Transactional // Rollback after test
    public void testEndToEndFlow() {
        // 1. Define Workflow
        WorkflowMaster wf = new WorkflowMaster();
        wf.setWorkflowName("Test Flow");
        wf.setWorkflowCode("TEST_001");
        wf.setAssociatedModule("Sales");
        wf.setSlaDurationDays(new BigDecimal("1.5"));
        workflowService.saveWorkflow(wf, "test-user");

        // 2. Define Stages
        StageConfig stage1 = new StageConfig();
        stage1.setWorkflowCode("TEST_001");
        stage1.setStageCode("STAGE_1");
        stage1.setStageName("Initial Review");
        stage1.setSequenceOrder(1);
        stage1.setNestedWorkflow(false);
        workflowService.saveStage(stage1, "test-user");

        // 3. Generate & Deploy
        deploymentService.deployWorkflow("TEST_001");

        // 4. Start Case
        ProcessInstance processInstance = flowableRuntime.startProcessInstanceByKey("TEST_001");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionKey()).isEqualTo("TEST_001");

        // 5. Verify Active
        ProcessInstance active = flowableRuntime.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(active).isNotNull();
        assertThat(active.isEnded()).isFalse();
    }
}
