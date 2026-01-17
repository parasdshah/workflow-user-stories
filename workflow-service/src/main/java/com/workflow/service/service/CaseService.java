package com.workflow.service.service;

import com.workflow.service.dto.CaseDTO;
import com.workflow.service.dto.StageDTO;
import com.workflow.service.repository.WorkflowMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.engine.history.HistoricActivityInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final WorkflowMasterRepository workflowRepository;
    private final com.workflow.service.repository.StageConfigRepository stageConfigRepository;
    private final com.workflow.service.integration.UserAdapterClient userAdapterClient;

    @Transactional
    public String initiateCase(String workflowCode, Map<String, Object> variables, String userId) {

        // Ensure workflow exists
        // Note: In a real system, we might check if a 'deployed' process definition
        // exists for this code
        // For now, we assume the workflowCode matches the Process Definition Key

        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put("initiator", userId);

        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(workflowCode, variables);
            log.info("Started process instance: {}", processInstance.getId());
            return processInstance.getId();
        } catch (org.flowable.common.engine.api.FlowableObjectNotFoundException e) {
            log.error("Workflow definition not found for code: {}", workflowCode, e);
            throw new IllegalArgumentException(
                    "Workflow Definition not found for code: " + workflowCode + ". Please ensure it is deployed.");
        } catch (Exception e) {
            log.error("Failed to start workflow: {}", workflowCode, e);
            throw new RuntimeException("Failed to start workflow: " + e.getMessage());
        }
    }

    public List<CaseDTO> getAllActiveCases(String workflowCode, String initiator, String cpId) {
        org.flowable.engine.runtime.ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc();

        if (workflowCode != null && !workflowCode.isEmpty()) {
            query.processDefinitionKey(workflowCode);
        }
        if (initiator != null && !initiator.isEmpty()) {
            query.variableValueEquals("initiator", initiator);
        }
        if (cpId != null && !cpId.isEmpty()) {
            query.variableValueEquals("cp_id", cpId);
        }

        List<ProcessInstance> instances = query.list();
        return instances.stream().map(this::mapToCaseDTO).collect(java.util.stream.Collectors.toList());
    }

    public CaseDTO getCaseDetails(String caseId) {
        // Try active process first
        ProcessInstance activeProcess = runtimeService.createProcessInstanceQuery()
                .processInstanceId(caseId)
                .singleResult();

        if (activeProcess != null) {
            return mapToCaseDTO(activeProcess);
        }

        // Try historic process
        HistoricProcessInstance historicProcess = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(caseId)
                .singleResult();

        if (historicProcess != null) {
            return mapToCaseDTO(historicProcess);
        }

        throw new IllegalArgumentException("Case not found with ID: " + caseId);
    }

    public List<StageDTO> getStages(String caseId) {
        List<StageDTO> stages = new ArrayList<>();

        // 1. Completed Stages (Historic Tasks)
        // 1. Completed Stages (Historic Tasks)
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(caseId)
                .finished()
                .includeTaskLocalVariables() // Fetch local vars for actionTaken
                .includeProcessVariables() // Fetch process vars for display
                .orderByTaskCreateTime().asc()
                .list();

        log.info("Found {} historic tasks for case {}", historicTasks.size(), caseId);
        for (HistoricTaskInstance task : historicTasks) {
            log.info("Historic Task: ID={}, Name={}, Assignee={}, EndTime={}", 
                    task.getId(), task.getName(), task.getAssignee(), task.getEndTime());
            stages.add(mapToStageDTO(task, "COMPLETED"));
        }

        // 2. Active Stages (Runtime Tasks)
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(caseId)
                .active()
                .includeProcessVariables() // Fetch process vars
                .orderByTaskCreateTime().asc()
                .list();

        for (Task task : activeTasks) {
            stages.add(mapToStageDTO(task, "ACTIVE"));
        }

        // 3. Call Activities (Sub-processes) - Historic
        // We need both active and completed access via HistoryService for activities
        List<HistoricActivityInstance> callActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(caseId)
                .activityType("callActivity")
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        for (HistoricActivityInstance activity : callActivities) {
            stages.add(mapToStageDTO(activity));
        }

        // Sort all by created time
        stages.sort(Comparator.comparing(StageDTO::getCreatedTime, Comparator.nullsLast(Comparator.naturalOrder())));

        resolveAssigneeNames(stages);

        return stages;
    }

    public List<StageDTO> getUserTaskHistory(String userId, String workflowCode, String cpId) {
        org.flowable.task.api.history.HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .includeTaskLocalVariables()
                .includeProcessVariables()
                .orderByHistoricTaskInstanceEndTime().desc();

        if (workflowCode != null && !workflowCode.isEmpty()) {
            query.processDefinitionKey(workflowCode);
        }
        if (cpId != null && !cpId.isEmpty()) {
            query.processVariableValueEquals("cp_id", cpId);
        }

        List<HistoricTaskInstance> tasks = query.list();

        List<StageDTO> result = new ArrayList<>();
        for (HistoricTaskInstance task : tasks) {
            StageDTO dto = mapToStageDTO(task, "COMPLETED");
            // Enrich with Case Name nicely? (Already handled in mapCommon via name/code,
            // but we might want Workflow Name here if needed.
            // For now, StageDTO contains task info and stage info.)
            result.add(dto);
        }
        resolveAssigneeNames(result);
        return result;
    }

    @Transactional
    public void completeTask(String taskId, Map<String, Object> variables, String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        String currentAssignee = task.getAssignee();
        log.info("Completing task {}. Current Assignee: {}", taskId, currentAssignee);

        try {
            org.flowable.common.engine.impl.identity.Authentication.setAuthenticatedUserId(userId);

            // Backup Assignee to Variable to guarantee persistence in history
            if (currentAssignee != null && !currentAssignee.trim().isEmpty()) {
                 taskService.setVariableLocal(taskId, "savedAssignee", currentAssignee);
            }

            // Logic to preserve or claim assignee
            if (currentAssignee == null || currentAssignee.trim().isEmpty()) {
                log.info("Task {} is unassigned. Auto-claim for user {}", taskId, userId);
                taskService.setAssignee(taskId, userId); 
                taskService.setVariableLocal(taskId, "savedAssignee", userId); // Ensure we save the claimed user
            } else {
                 // Force persistence attempt (keep for good measure, though variable is the real fix)
                taskService.setAssignee(taskId, currentAssignee);
            }

            // K. Stage Actions - Validate outcome
            if (variables != null && variables.containsKey("outcome")) {
                String outcome = (String) variables.get("outcome");

                // Get Flowable Task to get execution/process definition
                String processDefinitionId = task.getProcessDefinitionId();
                org.flowable.engine.repository.ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(processDefinitionId).singleResult();

                if (pd != null) {
                    String workflowCode = pd.getKey();
                    String stageCode = task.getTaskDefinitionKey();

                    // Fetch Stage Config
                    Optional<com.workflow.service.entity.StageConfig> configOpt = stageConfigRepository
                            .findByWorkflowCodeAndStageCode(workflowCode, stageCode);

                    if (configOpt.isPresent()) {
                        var actions = configOpt.get().getActions();
                        if (actions != null && !actions.isEmpty()) {
                            boolean isValid = actions.stream()
                                    .anyMatch(a -> a.getActionLabel().equals(outcome)); // Strict match on label

                            if (!isValid) {
                                throw new IllegalArgumentException(
                                        "Invalid outcome: " + outcome + ". Allowed actions: "
                                                + actions.stream()
                                                        .map(com.workflow.service.entity.StageAction::getActionLabel)
                                                        .collect(java.util.stream.Collectors.toList()));
                            }
                        }
                    }
                }
                // Save outcome as LOCAL variable to persist action for this specific task
                taskService.setVariableLocal(taskId, "outcome", outcome);
            }

            taskService.complete(taskId, variables);
            log.info("Task {} completed by {}", taskId, userId);
        } finally {
            org.flowable.common.engine.impl.identity.Authentication.setAuthenticatedUserId(null); // Clear context
        }
    }

    private CaseDTO mapToCaseDTO(ProcessInstance process) {
        CaseDTO dto = new CaseDTO();
        dto.setCaseId(process.getId());
        dto.setWorkflowCode(process.getProcessDefinitionKey());
        dto.setWorkflowName(process.getProcessDefinitionName()); // Might be null if not cached
        dto.setStatus("ACTIVE");
        dto.setStartTime(LocalDateTime.ofInstant(process.getStartTime().toInstant(), ZoneId.systemDefault()));
        dto.setStartUserId(process.getStartUserId());
        // Fetch parent via query since direct getter is missing on runtime interface
        ProcessInstance parent = runtimeService.createProcessInstanceQuery()
                .subProcessInstanceId(process.getId())
                .singleResult();
        if (parent != null) {
            dto.setParentCaseId(parent.getId());
        }

        // Enrich Name if null
        if (dto.getWorkflowName() == null) {
            workflowRepository.findByWorkflowCode(process.getProcessDefinitionKey())
                    .ifPresent(w -> dto.setWorkflowName(w.getWorkflowName()));
        }

        try {
            dto.setProcessVariables(runtimeService.getVariables(process.getId()));
        } catch (Exception e) {
            // ignore
        }

        return dto;
    }

    private CaseDTO mapToCaseDTO(HistoricProcessInstance process) {
        CaseDTO dto = new CaseDTO();
        dto.setCaseId(process.getId());
        dto.setWorkflowCode(process.getProcessDefinitionKey());
        dto.setWorkflowName(process.getProcessDefinitionName());
        dto.setStatus("ENDED");
        dto.setStartTime(LocalDateTime.ofInstant(process.getStartTime().toInstant(), ZoneId.systemDefault()));
        if (process.getEndTime() != null) {
            dto.setEndTime(LocalDateTime.ofInstant(process.getEndTime().toInstant(), ZoneId.systemDefault()));
        }
        dto.setStartUserId(process.getStartUserId());
        dto.setParentCaseId(process.getSuperProcessInstanceId());

        // Enrich Name
        if (dto.getWorkflowName() == null) {
            workflowRepository.findByWorkflowCode(process.getProcessDefinitionKey())
                    .ifPresent(w -> dto.setWorkflowName(w.getWorkflowName()));
        }

        try {
            List<org.flowable.variable.api.history.HistoricVariableInstance> vars = historyService
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(process.getId())
                    .list();
            Map<String, Object> varMap = new HashMap<>();
            for (org.flowable.variable.api.history.HistoricVariableInstance var : vars) {
                varMap.put(var.getVariableName(), var.getValue());
            }
            dto.setProcessVariables(varMap);
        } catch (Exception e) {
            // ignore
        }

        return dto;
    }

    private StageDTO mapToStageDTO(HistoricTaskInstance task, String status) {
        // Fallback Logic for Assignee
        String assignee = task.getAssignee();
        if (assignee == null && task.getTaskLocalVariables() != null && task.getTaskLocalVariables().containsKey("savedAssignee")) {
             assignee = (String) task.getTaskLocalVariables().get("savedAssignee");
             log.info("Recovered assignee {} from savedAssignee variable for task {}", assignee, task.getId());
        }

        StageDTO dto = mapCommonTaskInfo(task.getName(), task.getTaskDefinitionKey(), assignee,
                task.getCreateTime(), task.getDueDate(), task.getId(), task.getProcessDefinitionId(),
                task.getProcessInstanceId());
        dto.setStatus(status);
        if (task.getEndTime() != null) {
            dto.setEndTime(LocalDateTime.ofInstant(task.getEndTime().toInstant(), ZoneId.systemDefault()));
        }

        // Fetch Parent Case ID for History Grouping
        try {
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (hpi != null && hpi.getSuperProcessInstanceId() != null) {
                dto.setParentCaseId(hpi.getSuperProcessInstanceId());

                // Fetch Parent Process Instance to get Definition ID
                HistoricProcessInstance parentProcess = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(hpi.getSuperProcessInstanceId())
                        .singleResult();

                if (parentProcess != null) {
                    org.flowable.engine.repository.ProcessDefinition parentPd = repositoryService
                            .createProcessDefinitionQuery()
                            .processDefinitionId(parentProcess.getProcessDefinitionId())
                            .singleResult();
                    if (parentPd != null) {
                        dto.setParentWorkflowCode(parentPd.getKey());
                        dto.setParentWorkflowName(parentPd.getName());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore if fails, just grouping enhancement
        }

        // Map outcome to actionTaken from local variables
        if (task.getTaskLocalVariables() != null && task.getTaskLocalVariables().containsKey("outcome")) {
            dto.setActionTaken((String) task.getTaskLocalVariables().get("outcome"));
        }

        dto.setProcessVariables(task.getProcessVariables());
        return dto;
    }

    private StageDTO mapToStageDTO(Task task, String status) {
        StageDTO dto = mapCommonTaskInfo(task.getName(), task.getTaskDefinitionKey(), task.getAssignee(),
                task.getCreateTime(), task.getDueDate(), task.getId(), task.getProcessDefinitionId(),
                task.getProcessInstanceId());
        dto.setStatus(status);
        dto.setProcessVariables(task.getProcessVariables());
        return dto;
    }

    private StageDTO mapToStageDTO(HistoricActivityInstance activity) {
        StageDTO dto = new StageDTO();
        dto.setStageName(activity.getActivityName());
        dto.setStageCode(activity.getActivityId()); // ID in BPMN
        dto.setCaseId(activity.getProcessInstanceId());

        // Status mapping
        if (activity.getEndTime() != null) {
            dto.setStatus("COMPLETED");
            dto.setEndTime(LocalDateTime.ofInstant(activity.getEndTime().toInstant(), ZoneId.systemDefault()));
        } else {
            dto.setStatus("ACTIVE");
        }

        if (activity.getStartTime() != null) {
            dto.setCreatedTime(LocalDateTime.ofInstant(activity.getStartTime().toInstant(), ZoneId.systemDefault()));
        }

        // Populate Child Case ID
        dto.setSubProcessInstanceId(activity.getCalledProcessInstanceId());

        return dto;
    }

    public com.workflow.service.dto.GraphDTO getCaseGlobalGraph(String caseId) {
        // 1. Get Case to find Root Workflow
        CaseDTO caseDetails = getCaseDetails(caseId);
        String rootWorkflowCode = caseDetails.getWorkflowCode();

        // 2. Get Static Graph
        com.workflow.service.dto.GraphDTO graph = workflowDefinitionService.getGlobalGraph(rootWorkflowCode);

        // 3. Collect Runtime Status & Info
        Map<String, NodeRuntimeInfo> runtimeInfoMap = new HashMap<>();
        collectRuntimeInfo(caseId, rootWorkflowCode, runtimeInfoMap);

        // Resolve Assignees for Graph
        Set<String> allAssignees = new HashSet<>();
        runtimeInfoMap.values().forEach(info -> allAssignees.addAll(info.assignees));
        Map<String, String> resolvedNames = new HashMap<>();
        if (!allAssignees.isEmpty()) {
            resolvedNames = userAdapterClient.searchUsers(new ArrayList<>(allAssignees));
        }

        // 4. Enrich Graph Nodes
        for (com.workflow.service.dto.GraphDTO.NodeDTO node : graph.getNodes()) {
            // Calculate status
            NodeRuntimeInfo info = runtimeInfoMap.getOrDefault(node.getId(), new NodeRuntimeInfo("PENDING"));

            // Enrich Data
            Object originalData = node.getData();
            Map<String, Object> newData;
            if (originalData != null) {
                try {
                    newData = objectMapper.convertValue(originalData,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                            });
                } catch (Exception e) {
                    newData = new HashMap<>();
                    log.warn("Failed to convert node data to map", e);
                }
            } else {
                newData = new HashMap<>();
            }

            newData.put("status", info.status);
            if (!info.assignees.isEmpty()) {
                // Join assignees with comma or pass list.
                // Map IDs to Names
                Map<String, String> finalNames = resolvedNames;
                List<String> names = info.assignees.stream()
                    .map(id -> finalNames.getOrDefault(id, id))
                    .collect(java.util.stream.Collectors.toList());
                newData.put("assignee", String.join(", ", names));
                newData.put("assigneeIds", info.assignees); // Keep IDs if needed
            }
            node.setData(newData);
        }

        return graph;
    }

    private void collectRuntimeInfo(String processInstanceId, String prefix, Map<String, NodeRuntimeInfo> infoMap) {
        // Query History for all activities in this process instance
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        // Optimize: Fetch all historic tasks for this instance to get variables (savedAssignee)
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeTaskLocalVariables()
                .list();
        Map<String, HistoricTaskInstance> taskMap = historicTasks.stream()
                .collect(java.util.stream.Collectors.toMap(HistoricTaskInstance::getId, t -> t));

        for (HistoricActivityInstance activity : activities) {
            String nodeId = prefix + "_" + activity.getActivityId();
            
            NodeRuntimeInfo info = infoMap.computeIfAbsent(nodeId, k -> new NodeRuntimeInfo("PENDING"));

            // Status Logic: If any instance is Active -> Active. Else if any Completed -> Completed.
            String currentStatus = (activity.getEndTime() != null) ? "COMPLETED" : "ACTIVE";
            
            // Priority: ACTIVE > COMPLETED > PENDING
            if ("ACTIVE".equals(currentStatus)) {
                info.status = "ACTIVE";
            } else if ("COMPLETED".equals(currentStatus) && !"ACTIVE".equals(info.status)) {
                info.status = "COMPLETED";
            }

            // Collect Assignee
            String assignee = activity.getAssignee();
            if (assignee == null && activity.getTaskId() != null) {
                // Fallback: Check task local variables
                HistoricTaskInstance task = taskMap.get(activity.getTaskId());
                if (task != null && task.getTaskLocalVariables() != null && task.getTaskLocalVariables().containsKey("savedAssignee")) {
                    assignee = (String) task.getTaskLocalVariables().get("savedAssignee");
                }
            }

            if (assignee != null) {
                info.assignees.add(assignee);
            }

            // Recurse for Call Activities
            if ("callActivity".equals(activity.getActivityType()) && activity.getCalledProcessInstanceId() != null) {
                collectRuntimeInfo(activity.getCalledProcessInstanceId(), nodeId, infoMap);
            }
        }
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class NodeRuntimeInfo {
        String status;
        Set<String> assignees = new HashSet<>();
        
        public NodeRuntimeInfo(String status) {
            this.status = status;
        }
    }

    private StageDTO mapCommonTaskInfo(String name, String code, String assignee, Date createTime, Date dueDate,
            String taskId, String processDefinitionId, String processInstanceId) {
        StageDTO dto = new StageDTO();
        dto.setStageName(name);
        dto.setStageCode(code);
        dto.setTaskId(taskId);
        dto.setCaseId(processInstanceId);
        dto.setAssignee(assignee);
        if (createTime != null) {
            dto.setCreatedTime(LocalDateTime.ofInstant(createTime.toInstant(), ZoneId.systemDefault()));
        }
        if (dueDate != null) {
            dto.setDueDate(LocalDateTime.ofInstant(dueDate.toInstant(), ZoneId.systemDefault()));
        }

        // K. Stage Actions - Populate allowedActions
        try {
            if (processDefinitionId != null) {
                org.flowable.engine.repository.ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(processDefinitionId).singleResult();
                if (pd != null) {
                    dto.setWorkflowCode(pd.getKey()); // Populate workflowCode
                    stageConfigRepository.findByWorkflowCodeAndStageCode(pd.getKey(), code)
                            .ifPresent(config -> {
                                if (config.getActions() != null && !config.getActions().isEmpty()) {
                                    try {
                                        // Map to list of simple maps for JSON compatibility
                                        java.util.List<java.util.Map<String, String>> actionMaps = config.getActions()
                                                .stream()
                                                .map(a -> {
                                                    java.util.Map<String, String> m = new java.util.HashMap<>();
                                                    m.put("label", a.getActionLabel());
                                                    m.put("value", a.getActionLabel()); // Use label as value
                                                    m.put("style", a.getButtonStyle());
                                                    m.put("target", a.getTargetType());
                                                    m.put("postStatus", a.getPostActionStatus());
                                                    return m;
                                                })
                                                .collect(java.util.stream.Collectors.toList());
                                        dto.setAllowedActions(new com.fasterxml.jackson.databind.ObjectMapper()
                                                .writeValueAsString(actionMaps));
                                    } catch (Exception e) {
                                        log.warn("Failed to serialize actions for stage: {}", code, e);
                                    }
                                }
                            });
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch allowed actions for stage: {}", code, e);
        }

        return dto;
    }

    private void resolveAssigneeNames(List<StageDTO> stages) {
        List<String> assignees = stages.stream()
            .map(StageDTO::getAssignee)
            .filter(Objects::nonNull)
            .distinct()
            .collect(java.util.stream.Collectors.toList());

        log.info("Resolving names for assignees: {}", assignees);
            
        if (!assignees.isEmpty()) {
            Map<String, String> names = userAdapterClient.searchUsers(assignees);
            log.info("Resolved names map: {}", names);
            for (StageDTO stage : stages) {
                if (stage.getAssignee() != null) {
                    String name = names.get(stage.getAssignee());
                    if (name != null) {
                        stage.setAssigneeName(name);
                    }
                }
            }
        }
    }
    public List<com.workflow.service.dto.UserWorkloadDTO> getUserWorkload() {
        // 1. Fetch ALL active user tasks
        List<Task> activeTasks = taskService.createTaskQuery()
                .active()
                .includeProcessVariables()
                .orderByTaskCreateTime().desc()
                .list();

        // 2. Group by Assignee
        Map<String, List<Task>> tasksByAssignee = activeTasks.stream()
                .filter(t -> t.getAssignee() != null) // Ignore unassigned
                .collect(java.util.stream.Collectors.groupingBy(Task::getAssignee));

        if (tasksByAssignee.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Resolve Assignee Names
        Map<String, String> userNames = userAdapterClient.searchUsers(new ArrayList<>(tasksByAssignee.keySet()));

        // 4. Map to DTO
        List<com.workflow.service.dto.UserWorkloadDTO> result = new ArrayList<>();
        
        for (Map.Entry<String, List<Task>> entry : tasksByAssignee.entrySet()) {
            String userId = entry.getKey();
            List<Task> tasks = entry.getValue();
            String userName = userNames.getOrDefault(userId, userId);

            List<com.workflow.service.dto.UserWorkloadDTO.TaskSummaryDTO> taskSummaries = tasks.stream()
                    .map(t -> {
                        String workflowName = (String) t.getProcessVariables().get("workflowName");
                        if (workflowName == null) {
                             workflowName = t.getProcessDefinitionId().split(":")[0];
                        }

                        return com.workflow.service.dto.UserWorkloadDTO.TaskSummaryDTO.builder()
                                .taskId(t.getId())
                                .caseId(t.getProcessInstanceId())
                                .stageName(t.getName())
                                .stageCode(t.getTaskDefinitionKey())
                                .createdTime(LocalDateTime.ofInstant(t.getCreateTime().toInstant(), ZoneId.systemDefault()))
                                .dueDate(t.getDueDate() != null ? LocalDateTime.ofInstant(t.getDueDate().toInstant(), ZoneId.systemDefault()) : null)
                                .workflowName(workflowName)
                                .build();
                    })
                    .collect(java.util.stream.Collectors.toList());

            result.add(com.workflow.service.dto.UserWorkloadDTO.builder()
                    .userId(userId)
                    .userName(userName)
                    .pendingCount(tasks.size())
                    .tasks(taskSummaries)
                    .build());
        }

        // Sort by pending count desc
        result.sort((a, b) -> Integer.compare(b.getPendingCount(), a.getPendingCount()));

        return result;
    }
}
