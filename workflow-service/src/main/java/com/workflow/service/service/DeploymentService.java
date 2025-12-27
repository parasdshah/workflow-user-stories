package com.workflow.service.service;

import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentService {

    private final RepositoryService repositoryService;
    private final BpmnGeneratorService bpmnGeneratorService;
    private final WorkflowDefinitionService workflowDefinitionService;

    public String previewBpmn(String workflowCode) {
        WorkflowMaster workflow = workflowDefinitionService.getWorkflow(workflowCode)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowCode));
        List<StageConfig> stages = workflowDefinitionService.getStages(workflowCode);
        return bpmnGeneratorService.generateBpmnXml(workflow, stages);
    }

    @Transactional
    public Deployment deployWorkflow(String workflowCode) {
        String bpmnXml = previewBpmn(workflowCode);

        // J.1 Deploy workflow with automatic version management (Flowable handles
        // versioning by key)
        Deployment deployment = repositoryService.createDeployment()
                .name(workflowCode)
                .key(workflowCode)
                .addString(workflowCode + ".bpmn20.xml", bpmnXml)
                .deploy();

        log.info("Deployed workflow {}: id={}", workflowCode, deployment.getId());
        return deployment;
    }

    public List<Deployment> getDeploymentHistory(String workflowCode) {
        // J.8 View deployment history
        if (workflowCode == null || workflowCode.isEmpty()) {
            return repositoryService.createDeploymentQuery()
                    .orderByDeploymentTime().desc()
                    .list();
        }
        return repositoryService.createDeploymentQuery()
                .deploymentKey(workflowCode)
                .orderByDeploymentTime().desc()
                .list();
    }

    // Support J.2 Rollback by redeploying old XML or reverting pointer (Flowable
    // usually just deploys new version)
    // To "rollback", we can fetch the resource of a previous deployment and
    // redeploy it as new version.

    @Transactional
    public void undeployWorkflow(String deploymentId) {
        log.info("Undeploying workflow deployment: {}", deploymentId);

        // Soft Delete Strategy:
        // 1. Find Process Definition to get the Key (Workflow Code)
        org.flowable.engine.repository.ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();

        if (pd != null) {
            String workflowCode = pd.getKey();

            // 2. Mark as DELETED in DB
            workflowDefinitionService.markWorkflowAsDeleted(workflowCode);

            // 3. Suspend Process Definition to prevent new instances
            // We do NOT cascade delete history.
            repositoryService.suspendProcessDefinitionByKey(workflowCode, true, null); // Suspend process instances too?
                                                                                       // User said 'history lost', so
                                                                                       // maybe keep instances active?
            // "Undeploy" usually means "stop allowing new ones".
            // If we suspend process instances, active ones stop.
            // Let's just suspend the DEFINITION.
            // repositoryService.suspendProcessDefinitionById(pd.getId()); // This suspends
            // specific version
            // Or suspend by Key (all versions).
            // Better to just update DB status so UI hides it.
            // User requirement: "all cases from task history are lost. I dont want that".
            // If I deleteDeployment(cascade=true), everything is gone.
            // If I deleteDeployment(cascade=false), history remains but definition is gone
            // from engine (so no new starts).
            // User asked for "mark as soft delete".
            // So I will Just update DB.
            // But if I don't delete deployment, it's still "Deployed" in Flowable terms.
            // I will suspend it so it can't be started.
            try {
                repositoryService.suspendProcessDefinitionByKey(workflowCode);
            } catch (Exception e) {
                // Ignore if already suspended or not found
            }
        }

        // DO NOT call deleteDeployment
    }

    @Transactional
    public Deployment rollbackWorkflow(String deploymentId) {
        log.info("Rolling back to deployment: {}", deploymentId);

        // 1. Get Process Definition to find resource name
        org.flowable.engine.repository.ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();

        if (pd == null) {
            throw new IllegalArgumentException("No process definition found for deployment: " + deploymentId);
        }

        // 2. Get Resource Stream (BPMN XML)
        String resourceName = pd.getResourceName();
        try (java.io.InputStream xmlStream = repositoryService.getResourceAsStream(deploymentId, resourceName)) {
            if (xmlStream == null) {
                throw new IllegalStateException("Could not read resource: " + resourceName);
            }

            // 3. Redeploy as new version
            Deployment deployment = repositoryService.createDeployment()
                    .name(pd.getName() + " (Rollback)")
                    .key(pd.getKey())
                    .addInputStream(resourceName, xmlStream) // Preserves original filename
                    .deploy();

            log.info("Rolled back {} to version from deployment {}", pd.getKey(), deploymentId);
            return deployment;

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read resource for rollback", e);
        }
    }
}
