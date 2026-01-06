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
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import com.workflow.service.dto.WorkflowStatsDTO;
import java.util.stream.Collectors;
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
    private final RuntimeService runtimeService;
    private final HistoryService historyService;

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

    public List<WorkflowStatsDTO> getWorkflowStats() {
        return workflowRepository.findAll().stream().map(w -> {
            long active = 0;
            long completed = 0;
            // Only query if not deleted? Or query anyway to show historical capabilities?
            // User query: "how many active tasks are running on each workflow and how many
            // completed cases"
            // We use the Workflow Code (Process Definition Key)
            try {
                if (w.getWorkflowCode() != null) {
                    active = runtimeService.createProcessInstanceQuery()
                            .processDefinitionKey(w.getWorkflowCode())
                            .active()
                            .count();
                    completed = historyService.createHistoricProcessInstanceQuery()
                            .processDefinitionKey(w.getWorkflowCode())
                            .finished()
                            .count();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch stats for workflow: {}", w.getWorkflowCode(), e);
            }

            return WorkflowStatsDTO.builder()
                    .workflowCode(w.getWorkflowCode())
                    .workflowName(w.getWorkflowName())
                    .associatedModule(w.getAssociatedModule())
                    .status(w.getStatus())
                    .activeInstances(active)
                    .completedInstances(completed)
                    .build();
        }).collect(Collectors.toList());
    }

    public Optional<WorkflowMaster> getWorkflow(String code) {
        return workflowRepository.findByWorkflowCode(code);
    }

    @Transactional
    public void markWorkflowAsDeleted(String code) {
        workflowRepository.findByWorkflowCode(code).ifPresent(w -> {
            w.setStatus("DELETED");
            workflowRepository.save(w);
        });
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

    @Transactional
    public void deleteStage(String workflowCode, String stageCode) {
        StageConfig stage = stageRepository.findByWorkflowCodeAndStageCode(workflowCode, stageCode)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found"));
        // Additional cleanup if needed (e.g. screen mappings)
        screenMappingRepository.deleteByStageCode(stageCode);
        stageRepository.delete(stage);
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

    // Global Graph Generation
    public com.workflow.service.dto.GraphDTO getGlobalGraph(String rootWorkflowCode) {
        com.workflow.service.dto.GraphDTO graph = new com.workflow.service.dto.GraphDTO();
        java.util.Set<String> visited = new java.util.HashSet<>();

        // Start recursion with no parent (Root Level)
        buildGraphRecursively(rootWorkflowCode, null, graph, visited);

        return graph;
    }

    private void buildGraphRecursively(String workflowCode, String parentNodeId, com.workflow.service.dto.GraphDTO graph, java.util.Set<String> visited) {
        // Prevent infinite recursion (Cycles are blocked by save, but good to be safe)
        // We track "WorkflowCode within a specific path"?? 
        // Actually, a workflow CAN be called multiple times in different branches.
        // So simple "visited workflow code" is too strict. We need to stop if DEEP recursion cycle.
        // For now, let's limit depth or rely on the fact that saveStage blocks cycles.
        
        Optional<WorkflowMaster> wfOpt = workflowRepository.findByWorkflowCode(workflowCode);
        if (wfOpt.isEmpty()) return;

        List<StageConfig> stages = stageRepository.findByWorkflowCodeOrderBySequenceOrderAsc(workflowCode);
        
        // Context Prefix for IDs to ensure uniqueness across the flattened graph
        // If parentNodeId is "root:stage1", then children are "root:stage1:childStage"
        String prefix = (parentNodeId == null) ? workflowCode : parentNodeId;

        // Create Start Node
        String startNodeId = prefix + "_start";
        graph.getNodes().add(com.workflow.service.dto.GraphDTO.NodeDTO.builder()
                .id(startNodeId)
                .label("Start")
                .type("bpmnStart") // Use our custom types
                .parentId(parentNodeId)
                .position(new com.workflow.service.dto.GraphDTO.Position(0, 0)) // Layout will fix this
                .build());

        String previousNodeId = startNodeId;

        for (StageConfig stage : stages) {
            String currentNodeId = prefix + "_" + stage.getStageCode();
            String nodeType = "bpmnUserTask";
            String label = stage.getStageName();

            // Special Handling via Type
            if (stage.isNestedWorkflow()) {
                nodeType = "bpmnGroup"; // Special type for Container
                label = stage.getStageName() + " (Nested: " + stage.getNestedWorkflowCode() + ")";
                
                // Recurse!
                // The current node acts as the Parent for the child workflow
                buildGraphRecursively(stage.getNestedWorkflowCode(), currentNodeId, graph, visited);
            } else if (stage.isRuleStage()) {
                nodeType = "bpmnServiceTask";
                label = "Rule: " + stage.getRuleKey();
            }

            graph.getNodes().add(com.workflow.service.dto.GraphDTO.NodeDTO.builder()
                    .id(currentNodeId)
                    .label(label)
                    .type(nodeType)
                    .parentId(parentNodeId)
                    .data(stage) // Pass full stage config for details
                    .position(new com.workflow.service.dto.GraphDTO.Position(0, 0))
                    .build());

            // Edge from Previous -> Current
            graph.getEdges().add(com.workflow.service.dto.GraphDTO.EdgeDTO.builder()
                    .id("e_" + previousNodeId + "_" + currentNodeId)
                    .source(previousNodeId)
                    .target(currentNodeId)
                    .type("smoothstep")
                    .build());
            
            // Note: If this WAS a nested workflow, should we connect the "previous" to "child start"?
            // Visualizer option:
            // 1. Connect Parent-Prev -> Parent-Group-Container. (Standard)
            // 2. Connect Parent-Prev -> Child-Start. (Violates encapsulation, messy)
            // We stick to 1. The Group Node IS the node in the flow.
            
            // Connect Inner Flow logic?
            // If it's a Group, we want to visually connect the "Group Input" to "Inner Start"?
            // ReactFlow doesn't do "Port on Boundary" easily without custom handles.
            // For now, we rely on the visual nesting. The flow goes TO the group. 
            // Inside the group, it starts at Inner Start.
            
            previousNodeId = currentNodeId;
        }

        // Create End Node
        String endNodeId = prefix + "_end";
        graph.getNodes().add(com.workflow.service.dto.GraphDTO.NodeDTO.builder()
                .id(endNodeId)
                .label("End")
                .type("bpmnEnd")
                .parentId(parentNodeId)
                .position(new com.workflow.service.dto.GraphDTO.Position(0, 0))
                .build());

        // Edge from Last -> End
        graph.getEdges().add(com.workflow.service.dto.GraphDTO.EdgeDTO.builder()
                .id("e_" + previousNodeId + "_" + endNodeId)
                .source(previousNodeId)
                .target(endNodeId)
                .type("smoothstep")
                .build());
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
