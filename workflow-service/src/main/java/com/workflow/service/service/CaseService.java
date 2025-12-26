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
                .orderByTaskCreateTime().asc()
                .list();

        for (HistoricTaskInstance task : historicTasks) {
            stages.add(mapToStageDTO(task, "COMPLETED"));
        }

        // 2. Active Stages (Runtime Tasks)
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(caseId)
                .active()
                .orderByTaskCreateTime().asc()
                .list();

        for (Task task : activeTasks) {
            stages.add(mapToStageDTO(task, "ACTIVE"));
        }

        return stages;
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
                    String allowed = configOpt.get().getAllowedActions();
                    if (allowed != null && !allowed.isEmpty()) {
                        // Parse allowed actions (simple check for now, assuming JSON array or comma
                        // separated)
                        if (!allowed.contains(outcome)) {
                            throw new IllegalArgumentException(
                                    "Invalid outcome: " + outcome + ". Allowed actions: " + allowed);
                        }
                    }
                }
            }
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

        // Enrich Name if null
        if (dto.getWorkflowName() == null) {
            workflowRepository.findByWorkflowCode(process.getProcessDefinitionKey())
                    .ifPresent(w -> dto.setWorkflowName(w.getWorkflowName()));
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

        // Enrich Name
        if (dto.getWorkflowName() == null) {
            workflowRepository.findByWorkflowCode(process.getProcessDefinitionKey())
                    .ifPresent(w -> dto.setWorkflowName(w.getWorkflowName()));
        }

        return dto;
    }

    private StageDTO mapToStageDTO(HistoricTaskInstance task, String status) {
        StageDTO dto = mapCommonTaskInfo(task.getName(), task.getTaskDefinitionKey(), task.getAssignee(),
                task.getCreateTime(), task.getDueDate(), task.getId(), task.getProcessDefinitionId());
        dto.setStatus(status);
        if (task.getEndTime() != null) {
            dto.setEndTime(LocalDateTime.ofInstant(task.getEndTime().toInstant(), ZoneId.systemDefault()));
        }
        return dto;
    }

    private StageDTO mapToStageDTO(Task task, String status) {
        StageDTO dto = mapCommonTaskInfo(task.getName(), task.getTaskDefinitionKey(), task.getAssignee(),
                task.getCreateTime(), task.getDueDate(), task.getId(), task.getProcessDefinitionId());
        dto.setStatus(status);
        return dto;
    }

    private StageDTO mapCommonTaskInfo(String name, String code, String assignee, Date createTime, Date dueDate,
            String taskId, String processDefinitionId) {
        StageDTO dto = new StageDTO();
        dto.setStageName(name);
        dto.setStageCode(code);
        dto.setTaskId(taskId);
        dto.setAssignee(assignee);
        if (createTime != null) {
            dto.setCreatedTime(LocalDateTime.ofInstant(createTime.toInstant(), ZoneId.systemDefault()));
        }
        if (dueDate != null) {
            dto.setDueDate(LocalDateTime.ofInstant(dueDate.toInstant(), ZoneId.systemDefault()));
        }

        // K. Stage Actions - Populate allowedActions
        try {
            // Need workflow code.
            // If we have processDefinitionId, we can get it, or if it is cached.
            // For list optimization, we might need a better way, but for now:
            if (processDefinitionId != null) {
                // This might be expensive in a loop.
                // Ideally pass workflowCode if known, or cache.
                // For now, let's look up only if not null.
                // Optimization: Using repository cache if available
                // Simple approach:
                org.flowable.engine.repository.ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(processDefinitionId).singleResult();
                if (pd != null) {
                    stageConfigRepository.findByWorkflowCodeAndStageCode(pd.getKey(), code)
                            .ifPresent(config -> dto.setAllowedActions(config.getAllowedActions()));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch allowed actions for stage: {}", code, e);
        }

        return dto;
    }
}
