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
}
