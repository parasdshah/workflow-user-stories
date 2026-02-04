package com.workflow.service.service;

import com.workflow.service.integration.UserAdapterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentStrategyService {

    private final com.workflow.service.repository.StageConfigRepository stageConfigRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final UserAdapterClient userAdapterClient;
    private final HistoryService historyService;
    private final CalendarService calendarService;

    public void executeRoundRobin(DelegateTask delegateTask, String roleCode) {
        log.info("Executing Strategy: Round Robin for Role: {}", roleCode);

        // 1. Get Users in Pool
        List<String> candidates = userAdapterClient.getRoleMembers(roleCode);

        if (candidates == null || candidates.isEmpty()) {
            log.warn("No candidates found for Round Robin pool: {}", roleCode);
            // Consider leaving unassigned or setting candidate group?
            // "If no candidates, leave unassigned" is implicitly handled as we return.
            return;
        }

        // 2. Sort to ensure deterministic order
        Collections.sort(candidates);

        // --- STICKY CHECK ---
        if (isStickyConfigured(delegateTask)) {
             String stickyUser = findPriorActorInPool(delegateTask.getProcessInstanceId(), candidates);
             if (stickyUser != null) {
                 log.info("Sticky Round Robin: Found prior actor {}. Assigning.", stickyUser);
                 
                 // Availability Check
                 String effective = calendarService.getEffectiveAssignee(stickyUser);
                 if (!effective.equals(stickyUser)) {
                     log.info("Sticky Round Robin: User {} is OOO. Assigning to substitute {}", stickyUser, effective);
                     delegateTask.setAssignee(effective);
                 } else if (calendarService.getActiveLeave(stickyUser) != null) {
                      // OOO with no substitute -> Fallback to RR
                      log.info("Sticky Round Robin: User {} is OOO with no substitute. Falling back to standard Round Robin.", stickyUser);
                 } else {
                     delegateTask.setAssignee(stickyUser);
                     return;
                 }
             }
        }
        // --------------------

        // 3. Find Last Assignee for this Process Definition Key AND Task Definition Key
        String processDefinitionId = delegateTask.getProcessDefinitionId();
        String processDefinitionKey = processDefinitionId.split(":")[0];

        // Optimized Query: Fetch only the latest 1 finished task
        List<HistoricTaskInstance> lastTasks = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .taskDefinitionKey(delegateTask.getTaskDefinitionKey())
                .finished()
                .includeTaskLocalVariables() // Fetch local vars to get savedAssignee
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage(0, 1);

        String nextAssignee = candidates.get(0);
        String lastAssignee = null;

        if (!lastTasks.isEmpty()) {
            HistoricTaskInstance lastTask = lastTasks.get(0);
            lastAssignee = lastTask.getAssignee();

            // Fallback to savedAssignee if standard assignee is null (handling known
            // persistence issue)
            if (lastAssignee == null && lastTask.getTaskLocalVariables() != null
                    && lastTask.getTaskLocalVariables().containsKey("savedAssignee")) {
                lastAssignee = (String) lastTask.getTaskLocalVariables().get("savedAssignee");
                log.info("Round Robin: Recovered last assignee {} from variable", lastAssignee);
            }

            log.info("Round Robin: Found last assignee: {}", lastAssignee);

            if (lastAssignee != null) {
                int lastIdx = candidates.indexOf(lastAssignee);
                if (lastIdx != -1) {
                    int nextIdx = (lastIdx + 1) % candidates.size();
                    nextAssignee = candidates.get(nextIdx);
                } else {
                    log.warn("Round Robin: Last assignee {} not found in current pool {}. Resetting to first.",
                            lastAssignee, candidates);
                }
            }
        } else {
            log.info("Round Robin: No previous tasks found for this step. Assigning to first candidate.");
        }

        // DELEGATION LOGIC
        String finalAssignee = calendarService.getEffectiveAssignee(nextAssignee);
        if (!finalAssignee.equals(nextAssignee)) {
            log.info("Round Robin: Delegating from {} to {}", nextAssignee, finalAssignee);
        }

        log.info("Round Robin: Assigned {} from pool of {}", finalAssignee, candidates.size());
        delegateTask.setAssignee(finalAssignee);
    }
    
    private boolean isStickyConfigured(DelegateTask task) {
        try {
            String wf = task.getProcessDefinitionId().split(":")[0];
            String stg = task.getTaskDefinitionKey();
            var cfgOpt = stageConfigRepository.findByWorkflowCodeAndStageCode(wf, stg);
            if (cfgOpt.isPresent()) {
                String rulesJson = cfgOpt.get().getAssignmentRules();
                if (rulesJson != null && rulesJson.contains("\"variable\":\"sticky\"") && rulesJson.contains("\"value\":\"true\"")) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Error checking sticky config", e);
        }
        return false;
    }

    private String findPriorActorInPool(String currentPid, List<String> pool) {
        try {
            List<HistoricTaskInstance> hist = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(currentPid)
                    .finished()
                    .includeTaskLocalVariables()
                    .orderByHistoricTaskInstanceEndTime().desc()
                    .list();
            
            for (HistoricTaskInstance h : hist) {
                String u = h.getAssignee();
                if (u == null && h.getTaskLocalVariables() != null) {
                    u = (String) h.getTaskLocalVariables().get("savedAssignee");
                }
                
                if (u != null && pool.contains(u)) {
                    return u;
                }
            }
        } catch (Exception e) {
            log.error("Error finding prior actor", e);
        }
        return null;
    }
}
