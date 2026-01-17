package com.workflow.service.listener;

import com.workflow.service.dto.ResolutionRequest;
import com.workflow.service.integration.UserAdapterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;
import org.flowable.common.engine.api.delegate.Expression;

import java.util.Collections;
import java.util.List;

@Component("roundRobinAssignmentListener")
@RequiredArgsConstructor
@Slf4j
public class RoundRobinAssignmentListener implements TaskListener {

    private final UserAdapterClient userAdapterClient;
    private final HistoryService historyService;

    // Injected via Field Extension
    private Expression pool;

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            String roleCode = (String) pool.getValue(delegateTask);
            log.info("Executing Round Robin Assignment for Role: {}", roleCode);

            // 1. Get Users in Pool
            // ResolutionRequest req = new ResolutionRequest();
            // req.setRole(roleCode); 
            
            // We use getRoleMembers for Round Robin as we want ALL candidates
            List<String> candidates = userAdapterClient.getRoleMembers(roleCode);

            if (candidates == null || candidates.isEmpty()) {
                log.warn("No candidates found for Round Robin pool: {}", roleCode);
                return;
            }

            // 2. Sort to ensure deterministic order
            Collections.sort(candidates);

            // 3. Find Last Assignee for this Process Definition Key AND Task Definition Key
            String processDefinitionId = delegateTask.getProcessDefinitionId();
            String processDefinitionKey = processDefinitionId.split(":")[0];

            // Optimized Query: Fetch only the latest 1 finished task
            List<HistoricTaskInstance> lastTasks = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .taskDefinitionKey(delegateTask.getTaskDefinitionKey())
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage(0, 1);
            
            String nextAssignee = candidates.get(0);
            String lastAssignee = null;

            if (!lastTasks.isEmpty()) {
                lastAssignee = lastTasks.get(0).getAssignee();
                log.info("Round Robin: Found last assignee: {}", lastAssignee);
                
                if (lastAssignee != null) {
                    int lastIdx = candidates.indexOf(lastAssignee);
                    if (lastIdx != -1) {
                        int nextIdx = (lastIdx + 1) % candidates.size();
                        nextAssignee = candidates.get(nextIdx);
                    } else {
                         log.warn("Round Robin: Last assignee {} not found in current pool {}. Resetting to first.", lastAssignee, candidates);
                    }
                }
            } else {
                log.info("Round Robin: No previous tasks found. Assigning to first candidate.");
            }

            log.info("Round Robin: Assigned {} from pool of {}", nextAssignee, candidates.size());
            delegateTask.setAssignee(nextAssignee);

        } catch (Exception e) {
            log.error("Failed to execute Round Robin assignment", e);
        }
    }
}
