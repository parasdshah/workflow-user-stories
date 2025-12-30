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

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final WorkflowMasterRepository workflowRepository;
    private final com.workflow.service.repository.StageConfigRepository stageConfigRepository;

    @Transactional
    public String initiateCase(String workflowCode, Map<String, Object> variables, String userId) {
        log.info("Initiating case for workflow: {} by user: {}", workflowCode, userId);

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



    public List<CaseDTO> getAllActiveCases() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc()
                .list();
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
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(caseId)
                .finished()
                .finished()
                .includeTaskLocalVariables() // Fetch local vars for actionTaken
                .includeProcessVariables() // Fetch process vars for display
                .orderByTaskCreateTime().asc()
                .list();

        for (HistoricTaskInstance task : historicTasks) {
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

        return stages;
    }

    public List<StageDTO> getUserTaskHistory(String userId) {
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .includeTaskLocalVariables()
                .includeProcessVariables()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        List<StageDTO> result = new ArrayList<>();
        for (HistoricTaskInstance task : tasks) {
            StageDTO dto = mapToStageDTO(task, "COMPLETED");
            // Enrich with Case Name nicely? (Already handled in mapCommon via name/code,
            // but we might want Workflow Name here if needed.
            // For now, StageDTO contains task info and stage info.)
            result.add(dto);
        }
        return result;
    }

    @Transactional
    public void completeTask(String taskId, Map<String, Object> variables, String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        // Set assignee if not set (claim) or verify assignee?
        // For now, allow completion by user.
        taskService.setAssignee(taskId, userId); // Auto-claim

        // K. Stage Actions - Validate outcome
        if (variables != null && variables.containsKey("outcome")) {
            String outcome = (String) variables.get("outcome");

            // Get Flowable Task to get execution/process definition
            String processDefinitionId = task.getProcessDefinitionId();
            // We need Process Definition Key (Workflow Code)
            // We need Process Definition Key (Workflow Code)
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
                                    "Invalid outcome: " + outcome + ". Allowed actions: " + actions.stream().map(com.workflow.service.entity.StageAction::getActionLabel).collect(java.util.stream.Collectors.toList()));
                        }
                    }
                }
            }
            // Save outcome as LOCAL variable to persist action for this specific task
            taskService.setVariableLocal(taskId, "outcome", outcome);
        }

        taskService.complete(taskId, variables);
        log.info("Task {} completed by {}", taskId, userId);
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
            List<org.flowable.variable.api.history.HistoricVariableInstance> vars = historyService.createHistoricVariableInstanceQuery()
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
        StageDTO dto = mapCommonTaskInfo(task.getName(), task.getTaskDefinitionKey(), task.getAssignee(),
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
                                        java.util.List<java.util.Map<String, String>> actionMaps = config.getActions().stream()
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
                                        dto.setAllowedActions(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(actionMaps));
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
}
