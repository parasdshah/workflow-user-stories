package com.workflow.service.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

@Service("slaNotificationService")
@Slf4j
public class SlaNotificationService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String processId = execution.getProcessInstanceId();
        String currentActivityId = execution.getCurrentActivityId();

        log.warn("SLA BREACH DETECTED! Process ID: {}, Activity: {}", processId, currentActivityId);

        // TODO: Integrate with Notification Service (Email/SMS)
        // For now, setting a variable to verify in tests
        execution.setVariable("slaBreachNotified", true);
    }
}
