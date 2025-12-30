package com.workflow.service.service;

import com.workflow.service.repository.StageConfigRepository;
import com.workflow.service.repository.WorkflowMasterRepository;
import com.workflow.service.repository.AuditTrailRepository;
import com.workflow.service.repository.ScreenMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemResetService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService; // Can be used to check instances, but deleteDeployment handles cascade
    private final StageConfigRepository stageConfigRepository;
    private final WorkflowMasterRepository workflowMasterRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final ScreenMappingRepository screenMappingRepository;
    // Add other repositories as needed (e.g. AuditLog, Rules)

    @Transactional
    public void resetSystem() {
        log.warn("INITIATING SYSTEM RESET. ALL DATA WILL BE DELETED.");

        // 1. Undeploy All Workflows (Cascade to History/Runtime)
        try {
            List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
            for (Deployment d : deployments) {
                log.info("Deleting deployment: {} ({})", d.getId(), d.getName());
                repositoryService.deleteDeployment(d.getId(), true); // true = cascade
            }
            log.info("All Flowable deployments deleted.");
        } catch (Exception e) {
            log.error("Error deleting deployments", e);
            throw new RuntimeException("Failed to reset Flowable deployments", e);
        }

        // 2. Clean Custom Data
        try {
            log.info("Deleting all Stage Configs...");
            stageConfigRepository.deleteAll(); // Cascades to Stage Actions if configured correctly

            log.info("Deleting all Workflow Masters...");
            workflowMasterRepository.deleteAll();

            log.info("Deleting all Audit Trails...");
            auditTrailRepository.deleteAll();
            
            log.info("Deleting all Screen Mappings...");
            screenMappingRepository.deleteAll();
            
            log.info("Custom data cleaned.");
        } catch (Exception e) {
            log.error("Error cleaning custom data", e);
            throw new RuntimeException("Failed to reset custom data", e);
        }

        log.warn("SYSTEM RESET COMPLETED.");
    }
}
