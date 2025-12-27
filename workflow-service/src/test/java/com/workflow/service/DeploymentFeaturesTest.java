package com.workflow.service;

import com.workflow.service.service.DeploymentService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DeploymentFeaturesTest {

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private com.workflow.service.service.WorkflowDefinitionService workflowDefinitionService;

    @Autowired
    private com.workflow.service.repository.StageConfigRepository stageConfigRepository;

    @Autowired
    private com.workflow.service.repository.WorkflowMasterRepository workflowMasterRepository;

    private static final String WORKFLOW_CODE = "TEST_ADVANCED_DEPLOY";

    @BeforeEach
    public void cleanupAndSetup() {
        // Cleanup Deployments
        List<Deployment> deployments = repositoryService.createDeploymentQuery().deploymentKey(WORKFLOW_CODE).list();
        for (Deployment d : deployments) {
            repositoryService.deleteDeployment(d.getId(), true);
        }

        // Cleanup DB Data
        stageConfigRepository.deleteByWorkflowCode(WORKFLOW_CODE);
        workflowMasterRepository.findByWorkflowCode(WORKFLOW_CODE).ifPresent(workflowMasterRepository::delete);

        // Setup WorkflowMaster
        com.workflow.service.entity.WorkflowMaster wm = new com.workflow.service.entity.WorkflowMaster();
        wm.setWorkflowName("Test Advanced Deploy");
        wm.setWorkflowCode(WORKFLOW_CODE);
        workflowDefinitionService.saveWorkflow(wm, "test");

        // Setup Stage
        com.workflow.service.entity.StageConfig stage = new com.workflow.service.entity.StageConfig();
        stage.setWorkflowCode(WORKFLOW_CODE);
        stage.setStageCode("STAGE_01");
        stage.setStageName("Stage 1");
        stage.setSequenceOrder(1);
        // stage.setStageType("USER_TASK"); // Removed as field doesn't exist
        workflowDefinitionService.saveStage(stage, "test");
    }

    @Test
    public void testUndeployWorkflow() {
        // 1. Deploy
        Deployment deployment = deploymentService.deployWorkflow(WORKFLOW_CODE);
        assertThat(deployment).isNotNull();
        String deploymentId = deployment.getId();

        // 2. Verify exists
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId).count()).isEqualTo(1);

        // 3. Undeploy (Soft Delete)
        deploymentService.undeployWorkflow(deploymentId);

        // 4. Verify deployment STILL exists (Soft Delete)
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId).count()).isEqualTo(1);

        // 5. Verify WorkflowMaster status is DELETED
        com.workflow.service.entity.WorkflowMaster wm = workflowDefinitionService.getWorkflow(WORKFLOW_CODE)
                .orElseThrow();
        assertThat(wm.getStatus()).isEqualTo("DELETED");

        // 6. Verify Process Definition is Suspended
        org.flowable.engine.repository.ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId).singleResult();
        assertThat(pd.isSuspended()).isTrue();
    }

    @Test
    public void testRollbackWorkflow() {
        // 1. Deploy Version 1
        // We need different BPMN content to distinguish versions physically if needed,
        // but for rollback logic we are testing the RE-DEPLOY mechanism of an existing
        // resource.
        // DeploymentService.deployWorkflow uses current config.

        Deployment v1 = deploymentService.deployWorkflow(WORKFLOW_CODE);
        assertThat(v1).isNotNull();
        String v1Id = v1.getId();

        // Sleep to ensure timestamp diff if necessary

        // 2. Deploy Version 2
        // Just deploying again creates a new version in Flowable
        Deployment v2 = deploymentService.deployWorkflow(WORKFLOW_CODE);
        assertThat(v2).isNotNull();
        String v2Id = v2.getId();

        assertThat(v2Id).isNotEqualTo(v1Id);

        // 3. Rollback to V1
        // This should create V3 which has content of V1
        Deployment v3 = deploymentService.rollbackWorkflow(v1Id);

        assertThat(v3).isNotNull();
        assertThat(v3.getId()).isNotEqualTo(v1Id);
        assertThat(v3.getId()).isNotEqualTo(v2Id);

        // Verify name indicates rollback
        assertThat(v3.getName()).contains("(Rollback)");
    }
}
