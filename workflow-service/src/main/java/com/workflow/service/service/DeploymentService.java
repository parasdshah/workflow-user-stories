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

    public List<com.workflow.service.dto.DeploymentHistoryDTO> getDeploymentHistory(String workflowCode) {
        // J.8 View deployment history
        List<Deployment> deployments;
        if (workflowCode == null || workflowCode.isEmpty()) {
            deployments = repositoryService.createDeploymentQuery()
                    .orderByDeploymentTime().desc()
                    .list();
        } else {
            deployments = repositoryService.createDeploymentQuery()
                    .deploymentKey(workflowCode)
                    .orderByDeploymentTime().desc()
                    .list();
        }

        return deployments.stream().map(d -> {
            String status = "ACTIVE";
            org.flowable.engine.repository.ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(d.getId())
                    .singleResult();
            if (pd != null && pd.isSuspended()) {
                status = "SUSPENDED";
            }
            // If PD is null, it might be a purely resource deployment or something went
            // wrong, but "ACTIVE" or "UNKNOWN"
            // We'll assume ACTIVE or maybe UNKNOWN. Let's stick to ACTIVE unless suspended.

            return com.workflow.service.dto.DeploymentHistoryDTO.builder()
                    .id(d.getId())
                    .name(d.getName())
                    .deploymentTime(d.getDeploymentTime())
                    .status(status)
                    .build();
        }).collect(java.util.stream.Collectors.toList());
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

            // Check if this is the latest version
            org.flowable.engine.repository.ProcessDefinition latest = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(workflowCode)
                    .latestVersion()
                    .singleResult();

            boolean isLatest = latest != null && latest.getId().equals(pd.getId());

            if (isLatest) {
                // If it's the latest version, we suspend the Key to stop new instances.
                // We do NOT mark as DELETED in DB here (that's for Delete Workflow).
                log.info("Suspending latest version (Key: {})", workflowCode);
                repositoryService.suspendProcessDefinitionByKey(workflowCode, true, null);
            } else {
                // If it's an old version, we suspend just this version ID.
                log.info("Suspending old version (ID: {})", pd.getId());
                repositoryService.suspendProcessDefinitionById(pd.getId(), true, null);
            }
        }
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
