package com.workflow.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component("preExitDelegate")
public class PreExitDelegate implements JavaDelegate, TaskListener {
    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("preExitDelegate (JavaDelegate) executed! Instance ID: " + execution.getProcessInstanceId());
        execution.setVariable("delegateExecuted", true);
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        System.out.println("preExitDelegate (TaskListener) executed! Task Name: " + delegateTask.getName());
        delegateTask.setVariable("hookExecuted", true);
    }
}
