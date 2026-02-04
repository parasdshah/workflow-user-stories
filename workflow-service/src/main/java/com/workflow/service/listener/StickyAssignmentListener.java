package com.workflow.service.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.integration.UserAdapterClient;
import com.workflow.service.repository.StageConfigRepository;
import com.workflow.service.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService; 
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("stickyAssignmentListener")
@org.springframework.context.annotation.Scope(org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class StickyAssignmentListener implements TaskListener {

    private final UserAdapterClient userAdapterClient;
    private final HistoryService historyService;
    private final CalendarService calendarService;
    private final StageConfigRepository stageConfigRepository;
    private final ObjectMapper objectMapper;
    private final RuntimeService runtimeService; // Inject RuntimeService

    // "role" can be passed as Field Extension to know WHICH role group to look for
    @lombok.Setter
    private Expression role;

    @Override
    public void notify(DelegateTask delegateTask) {
        log.error(">>> STICKY LISTENER TRIGGERED for Task: {} (ID: {}) <<<", delegateTask.getName(), delegateTask.getId());
        String roleCode = null;
        try {
            if (role != null) {
                roleCode = (String) role.getValue(delegateTask);
            }

            if (roleCode == null) {
                log.warn("Sticky Assignment: No Role configured. Cannot execute.");
                return;
            }

            log.info("Executing Sticky Assignment for Role: {}", roleCode);

            // 1. Get Valid Candidates
            List<String> currentCandidates = userAdapterClient.getRoleMembers(roleCode);
            if (currentCandidates == null || currentCandidates.isEmpty()) {
                log.warn("Sticky Assignment: No candidates found for Role {}. Cannot assign.", roleCode);
                return;
            }

            // 2. CHECK IMMEDIATE PREDECESSOR (Runtime)
            String currentUser = org.flowable.common.engine.impl.identity.Authentication.getAuthenticatedUserId();
            if (currentUser != null && currentCandidates.contains(currentUser)) {
                log.info("Sticky Assignment: Current authenticated user {} is a valid candidate. Assigning immediately.", currentUser);
                delegateTask.setAssignee(currentUser);
                return;
            }

            // 3. Find Prior Actor from HISTORY
            String processInstanceId = delegateTask.getProcessInstanceId();
            String processDefinitionId = delegateTask.getProcessDefinitionId();
            
            // Try to lookup Business Key correctly using RuntimeService
            String businessKey = null;
            try {
                 // Try RuntimeService first (active process)
                 org.flowable.engine.runtime.ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                 if (processInstance != null) {
                     businessKey = processInstance.getBusinessKey();
                 }
                
                // Fallback to History if runtime failed (or not found)
                if (businessKey == null) {
                     org.flowable.engine.history.HistoricProcessInstance currentInst = historyService.createHistoricProcessInstanceQuery()
                             .processInstanceId(processInstanceId)
                             .singleResult();
                     if (currentInst != null) businessKey = currentInst.getBusinessKey();
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve Business Key", e);
            }

            log.info("Sticky Assignment: PID: {}, BusinessKey: {}", processInstanceId, businessKey);

            List<HistoricTaskInstance> history;
            if (businessKey != null) {
                history = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceBusinessKey(businessKey)
                        .finished()
                        .includeTaskLocalVariables() // CRITICAL: Fetch variables to recover savedAssignee
                        .orderByHistoricTaskInstanceEndTime().desc()
                        .list();
            } else {
                history = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .finished()
                        .includeTaskLocalVariables() // CRITICAL: Fetch variables to recover savedAssignee
                        .orderByHistoricTaskInstanceEndTime().desc()
                        .list();
            }

            String stickyUser = null;
            log.info("Sticky Assignment: History Query found {} tasks.", history.size());

            // --- DEBUG: Print Full History Trace ---
            log.info("==================== HISTORY TRACE ====================");
            log.info(String.format("%-20s | %-20s | %-15s | %-15s | %-15s", "Task Name", "Stage Code", "User", "Config Role", "Workflow"));
            for (HistoricTaskInstance h : history) {
                try {
                     String wf = h.getProcessDefinitionId() != null ? h.getProcessDefinitionId().split(":")[0] : "N/A";
                     String stg = h.getTaskDefinitionKey();
                     
                     // Helper Logic for Assignee Recovery
                     String usr = h.getAssignee();
                     if (usr == null && h.getTaskLocalVariables() != null) {
                         usr = (String) h.getTaskLocalVariables().get("savedAssignee");
                     }
                     
                     String role = "UNKNOWN";
                     Optional<StageConfig> cfg = stageConfigRepository.findByWorkflowCodeAndStageCode(wf, stg);
                     if (cfg.isPresent()) {
                         role = getTaskRole(cfg.get());
                     }
                     
                     log.info(String.format("%-20s | %-20s | %-15s | %-15s | %-15s", h.getName(), stg, usr, role, wf));
                } catch (Exception e) {
                    log.error("Error logging trace for task {}", h.getId(), e);
                }
            }
            log.info("=======================================================");
            // ----------------------------------------

            for (HistoricTaskInstance histTask : history) {
                // Recover Assignee
                String potentialUser = histTask.getAssignee();
                if (potentialUser == null && histTask.getTaskLocalVariables() != null) {
                    potentialUser = (String) histTask.getTaskLocalVariables().get("savedAssignee");
                    if (potentialUser != null) {
                        log.info(" - Recovered Assignee {} from 'savedAssignee' var for task {}", potentialUser, histTask.getName());
                    }
                }
                
                // Safe check for null definition ID
                if (histTask.getProcessDefinitionId() == null) continue;
                
                String histWfCode = histTask.getProcessDefinitionId().split(":")[0];
                String stageCode = histTask.getTaskDefinitionKey();
                
                log.info(" - Checking: {} (User: {})", histTask.getName(), potentialUser);

                if (potentialUser == null || !currentCandidates.contains(potentialUser)) {
                    log.info("   -> Skipped: User null or not in current candidate list for Role {}", roleCode);
                    continue; 
                }

                // Lookup StageConfig using the HISTORIC task's workflow code
                Optional<StageConfig> configOpt = stageConfigRepository.findByWorkflowCodeAndStageCode(histWfCode, stageCode);
                if (configOpt.isPresent()) {
                    StageConfig config = configOpt.get();
                    String configRole = getTaskRole(config); // Use helper for logging
                    
                    if (isStageForRole(config, roleCode)) {
                        stickyUser = potentialUser;
                        log.info("   -> MATCH! Found prior actor {} from task {} (Stage matched Role: {})", stickyUser, histTask.getName(), configRole);
                        break;
                    } else {
                        log.info("   -> Mismatch: Stage {} is configured for Role '{}', but we need '{}'", stageCode, configRole, roleCode);
                        log.info("      -> Rules JSON: {}", config.getAssignmentRules());
                    }
                } else {
                    log.info("   -> Warning: No StageConfig found for StageCode: {} in Workflow: {}", stageCode, histWfCode);
                }
            }

            if (stickyUser != null) {
                // 3. Availability Check
                String effectiveUser = calendarService.getEffectiveAssignee(stickyUser);

                if (calendarService.getActiveLeave(stickyUser) != null) {
                    // User is OOO
                    if (effectiveUser.equals(stickyUser)) {
                        // Means no substitute defined.
                        log.info("Sticky Assignment: User {} is OOO with no substitute. No Assignment (Fallback removed).", stickyUser);
                        // User requested REMOVAL of fallback. So we do nothing.
                        return;
                    } else {
                        // User OOO but has substitute.
                        log.info("Sticky Assignment: assigning to substitute {}", effectiveUser);
                        delegateTask.setAssignee(effectiveUser);
                        return;
                    }
                } else {
                    // User Available
                    delegateTask.setAssignee(stickyUser);
                    return;
                }
            }

            // 4. Fallback: No Prior Actor found
            log.info("Sticky Assignment: No prior actor found for Role {}. No Assignment (Fallback removed).", roleCode);
            // User requested REMOVAL of fallback. So we do nothing.

        } catch (Exception e) {
            log.error("Failed to execute Sticky assignment", e);
        }
    }

    private String getTaskRole(StageConfig config) {
        if (config.getAssignmentRules() == null || config.getAssignmentRules().isBlank()) return "N/A";
        try {
            Map<String, Object> rules = objectMapper.readValue(config.getAssignmentRules(), new TypeReference<Map<String, Object>>() {});
            String role = (String) rules.getOrDefault("role", null);
            if (role == null) role = (String) rules.getOrDefault("groupName", null);
            if (role == null) role = (String) rules.getOrDefault("roundRobinPool", null);
            if (role == null) role = (String) rules.getOrDefault("matrixRole", null);
            return role != null ? role : "N/A";
        } catch (Exception e) {
            return "Error";
        }
    }

    private boolean isStageForRole(StageConfig config, String targetRole) {
        String role = getTaskRole(config);
        return targetRole.equals(role);
    }
}
