package com.workflow.service.service;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
//import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
//import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuntimeStatusService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    // HistoryService historyService; (Optional for finished cases)

    @Transactional
    public ProcessInstance startCase(String workflowCode, Map<String, Object> variables) {
        // D.1 Start Case
        return runtimeService.startProcessInstanceByKey(workflowCode, variables);
    }

    public ProcessInstance getCaseStatus(String caseId) {
        // D.1 View overall case status
        return runtimeService.createProcessInstanceQuery().processInstanceId(caseId).singleResult();
    }

    public List<Task> getCaseTasks(String caseId) {
        // D.2 View stage status with assignees
        return taskService.createTaskQuery().processInstanceId(caseId).list();
    }

    public List<ProcessInstance> getAllCases() {
        return runtimeService.createProcessInstanceQuery().list();
    }
}
