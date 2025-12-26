package com.workflow.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.service.entity.AuditTrail;
import com.workflow.service.entity.ScreenMapping;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
import com.workflow.service.repository.AuditTrailRepository;
import com.workflow.service.repository.ScreenMappingRepository;
import com.workflow.service.repository.StageConfigRepository;
import com.workflow.service.repository.WorkflowMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDefinitionService {

    private final WorkflowMasterRepository workflowRepository;
    private final StageConfigRepository stageRepository;
    private final ScreenMappingRepository screenMappingRepository;
    private final com.workflow.service.repository.ScreenDefinitionRepository screenDefinitionRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final ObjectMapper objectMapper;

    // Workflow Master CRUD

    @Transactional
    public WorkflowMaster saveWorkflow(WorkflowMaster workflow, String user) {
        boolean isNew = workflow.getId() == null;
        WorkflowMaster saved = workflowRepository.save(workflow);
        logAudit("WorkflowMaster", saved.getId().toString(), isNew ? "CREATE" : "UPDATE", user, workflow);
        return saved;
    }

    public List<WorkflowMaster> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    public Optional<WorkflowMaster> getWorkflow(String code) {
        return workflowRepository.findByWorkflowCode(code);
    }

    // Stage Config CRUD

    @Transactional
    public StageConfig saveStage(StageConfig stage, String user) {
        // Validation: Check Cycle (DFS)
        if (stage.isNestedWorkflow()) {
            if (stage.getNestedWorkflowCode().equals(stage.getWorkflowCode())) {
                throw new IllegalArgumentException(
                        "Cyclic dependency detected: Nested workflow cannot be the same as parent workflow.");
            }
            detectCycle(stage.getWorkflowCode(), stage.getNestedWorkflowCode());
        }

        // Validate Hooks
        validateHooks(stage);

        boolean isNew = stage.getId() == null;
        StageConfig saved = stageRepository.save(stage);
        logAudit("StageConfig", saved.getId().toString(), isNew ? "CREATE" : "UPDATE", user, stage);
        return saved;
    }

    public List<StageConfig> getStages(String workflowCode) {
        return stageRepository.findByWorkflowCodeOrderBySequenceOrderAsc(workflowCode);
    }

    // Screen Mapping CRUD

    @Transactional
    public ScreenMapping saveScreenMapping(ScreenMapping mapping, String user) {
        boolean isNew = mapping.getId() == null;
        ScreenMapping saved = screenMappingRepository.save(mapping);
        logAudit("ScreenMapping", saved.getId().toString(), isNew ? "CREATE" : "UPDATE", user, mapping);
        return saved;
    }

    public List<ScreenMapping> getScreenMappings(String stageCode) {
        return screenMappingRepository.findByStageCode(stageCode);
    }

    // Screen Definition CRUD

    @Transactional
    public com.workflow.service.entity.ScreenDefinition saveScreenDefinition(
            com.workflow.service.entity.ScreenDefinition screen, String user) {
        boolean isNew = screen.getCreatedAt() == null; // Simple check since ID is assigned manually
        com.workflow.service.entity.ScreenDefinition saved = screenDefinitionRepository.save(screen);
        logAudit("ScreenDefinition", saved.getScreenCode(), isNew ? "CREATE" : "UPDATE", user, screen);
        return saved;
    }

    public List<com.workflow.service.entity.ScreenDefinition> getAllScreenDefinitions() {
        return screenDefinitionRepository.findAll();
    }

    public Optional<com.workflow.service.entity.ScreenDefinition> getScreenDefinition(String code) {
        return screenDefinitionRepository.findById(code);
    }

    // Internal: Cycle Detection (DFS)
    private void detectCycle(String sourceWorkflow, String targetWorkflow) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        visited.add(sourceWorkflow);
        dfsCheck(targetWorkflow, visited);
    }

    private void dfsCheck(String currentWorkflow, java.util.Set<String> visited) {
        if (visited.contains(currentWorkflow)) {
            throw new IllegalArgumentException("Cyclic dependency detected involving workflow: " + currentWorkflow);
        }
        visited.add(currentWorkflow);

        // Find all stages in the current workflow that call other workflows
        List<StageConfig> stages = stageRepository.findByWorkflowCodeOrderBySequenceOrderAsc(currentWorkflow);
        for (StageConfig stage : stages) {
            if (stage.isNestedWorkflow() && stage.getNestedWorkflowCode() != null) {
                dfsCheck(stage.getNestedWorkflowCode(), new java.util.HashSet<>(visited)); // Pass copy of path
            }
        }
    }

    // Audit Helper
    private void logAudit(String entityName, String entityId, String action, String user, Object payload) {
        try {
            AuditTrail audit = new AuditTrail();
            audit.setEntityName(entityName);
            audit.setEntityId(entityId);
            audit.setAction(action);
            audit.setChangedBy(user);
            audit.setChanges(objectMapper.writeValueAsString(payload));
            auditTrailRepository.save(audit);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit payload", e);
        }
    }

    private void validateHooks(StageConfig stage) {
        validateClassExists(stage.getPreEntryHook());
        validateClassExists(stage.getPostEntryHook());
        validateClassExists(stage.getPreExitHook());
        validateClassExists(stage.getPostExitHook());
    }

    private void validateClassExists(String fqn) {
        if (fqn == null || fqn.isBlank())
            return;
        try {
            Class.forName(fqn);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Hook class not found on classpath: " + fqn);
        }
    }
}
