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
            ResolutionRequest req = new ResolutionRequest();
            req.setRole(roleCode); // Fixed: String setter
            
            // We assume Adapter handles "Find all users with this role" if other criteria are empty
            List<String> candidates = userAdapterClient.resolveUsers(req);

            if (candidates == null || candidates.isEmpty()) {
                log.warn("No candidates found for Round Robin pool: {}", roleCode);
                return;
            }

            // 2. Sort to ensure deterministic order
            Collections.sort(candidates);

            // 3. Find Last Assignee for this Process Definition Key AND Task Definition Key
            String processDefinitionId = delegateTask.getProcessDefinitionId();
            // Standard Flowable ID format: Key:Version:Id
            // Note: If ID format is different, fallback to exact match?
            // Safer: Use process ID but filtering by Key is better for across versions.
            String processDefinitionKey = processDefinitionId.split(":")[0];

            // Find last *completed* task for this specific stage in this workflow type
            List<HistoricTaskInstance> lastTasks = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .taskDefinitionKey(delegateTask.getTaskDefinitionKey())
                .finished()
                .list();
            
            // Sort by End Time DESC (Java side to avoid API version mismatch)
            lastTasks.sort((t1, t2) -> {
                if (t1.getEndTime() == null) return 1;
                if (t2.getEndTime() == null) return -1;
                return t2.getEndTime().compareTo(t1.getEndTime());
            });

            String nextAssignee = candidates.get(0);

            if (!lastTasks.isEmpty()) {
                String lastAssignee = lastTasks.get(0).getAssignee();
                if (lastAssignee != null) {
                    int lastIdx = candidates.indexOf(lastAssignee);
                    if (lastIdx != -1) {
                        int nextIdx = (lastIdx + 1) % candidates.size();
                        nextAssignee = candidates.get(nextIdx);
                    }
                }
            }

            log.info("Round Robin: Assigned {} from pool of {}", nextAssignee, candidates.size());
            delegateTask.setAssignee(nextAssignee);

        } catch (Exception e) {
            log.error("Failed to execute Round Robin assignment", e);
        }
    }
}
