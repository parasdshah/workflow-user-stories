package com.workflow.service.listener;

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
@org.springframework.context.annotation.Scope(org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class RoundRobinAssignmentListener implements TaskListener {

    private final com.workflow.service.service.AssignmentStrategyService assignmentStrategyService;

    // Injected via Field Extension
    @lombok.Setter
    private Expression pool;

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            String roleCode = (String) pool.getValue(delegateTask);
            assignmentStrategyService.executeRoundRobin(delegateTask, roleCode);
        } catch (Exception e) {
            log.error("Failed to execute Round Robin assignment", e);
        }
    }
}
