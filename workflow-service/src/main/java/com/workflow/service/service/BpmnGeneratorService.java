package com.workflow.service.service;

import com.workflow.service.entity.ScreenMapping;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
import com.workflow.service.repository.ScreenMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.BpmnAutoLayout;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BpmnGeneratorService {

    private final ScreenMappingRepository screenMappingRepository;

    public String generateBpmnXml(WorkflowMaster workflow, List<StageConfig> stages) {
        BpmnModel model = new BpmnModel();
        Process process = new Process();
        process.setId(workflow.getWorkflowCode());
        process.setName(workflow.getWorkflowName());
        process.setExecutable(true);
        model.addProcess(process);

        // Start Event
        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        process.addFlowElement(startEvent);

        FlowElement previousElement = startEvent;

        for (StageConfig stage : stages) {
            FlowElement stageElement;
            if (stage.isNestedWorkflow()) {
                CallActivity callActivity = new CallActivity();
                callActivity.setCalledElement(stage.getNestedWorkflowCode());
                stageElement = callActivity;
            } else {
                UserTask userTask = new UserTask();
                // Requirement G.2: Use mapped screen code as form key
                String formKey = getFormKeyForStage(stage.getStageCode());
                userTask.setFormKey(formKey);
                stageElement = userTask;
            }

            stageElement.setId(stage.getStageCode());
            stageElement.setName(stage.getStageName());

            // Hooks (Listeners) - Requirement G.4 & B.3
            // 1. Pre-Entry (ExecutionListener - start)
            if (stage.getPreEntryHook() != null && !stage.getPreEntryHook().isBlank()) {
                stageElement.setExecutionListeners(List.of(createListener(stage.getPreEntryHook(), "start")));
            }
            // 2. Post-Exit (ExecutionListener - end)
            if (stage.getPostExitHook() != null && !stage.getPostExitHook().isBlank()) {
                stageElement.setExecutionListeners(List.of(createListener(stage.getPostExitHook(), "end")));
            }

            // 3. Post-Entry & Pre-Exit (TaskListeners) - Only for UserTask
            if (stageElement instanceof UserTask) {
                UserTask userTask = (UserTask) stageElement;
                if (stage.getPostEntryHook() != null && !stage.getPostEntryHook().isBlank()) {
                    // postEntry -> create event
                    FlowableListener listener = createListener(stage.getPostEntryHook(), "create");
                    userTask.getTaskListeners().add(listener);
                }
                if (stage.getPreExitHook() != null && !stage.getPreExitHook().isBlank()) {
                    // preExit -> complete event
                    FlowableListener listener = createListener(stage.getPreExitHook(), "complete");
                    userTask.getTaskListeners().add(listener);
                }
            }

            process.addFlowElement(stageElement);

            // Sequence Flow
            SequenceFlow flow = new SequenceFlow();
            flow.setSourceRef(previousElement.getId());
            flow.setTargetRef(stageElement.getId());
            process.addFlowElement(flow);

            // Requirement G.5: SLA Timer Boundary Event
            BigDecimal slaDays = stage.getSlaDurationDays();
            if (slaDays == null || slaDays.compareTo(BigDecimal.ZERO) <= 0) {
                // Fallback to global workflow SLA
                if (workflow.getSlaDurationDays() != null
                        && workflow.getSlaDurationDays().compareTo(BigDecimal.ZERO) > 0) {
                    slaDays = workflow.getSlaDurationDays();
                }
            }

            if (slaDays != null && slaDays.compareTo(BigDecimal.ZERO) > 0) {
                if (stageElement instanceof UserTask) {
                    addSlaTimer(process, (UserTask) stageElement, slaDays);
                }
            }

            previousElement = stageElement;
        }

        // Requirement G.6: Completion Mapping
        if (workflow.getCompletionApiEndpoint() != null && !workflow.getCompletionApiEndpoint().isBlank()) {
            ServiceTask completionTask = new ServiceTask();
            completionTask.setId("completionServiceTask");
            completionTask.setName("Call Completion API");
            completionTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            completionTask.setImplementation("${completionService}");

            process.addFlowElement(completionTask);

            SequenceFlow toCompletion = new SequenceFlow();
            toCompletion.setSourceRef(previousElement.getId());
            toCompletion.setTargetRef(completionTask.getId());
            process.addFlowElement(toCompletion);

            previousElement = completionTask;
        }

        // End Event
        EndEvent endEvent = new EndEvent();
        endEvent.setId("end");
        process.addFlowElement(endEvent);

        SequenceFlow endFlow = new SequenceFlow();
        endFlow.setSourceRef(previousElement.getId());
        endFlow.setTargetRef(endEvent.getId());
        process.addFlowElement(endFlow);

        // Auto Layout (Generate DI)
        new BpmnAutoLayout(model).execute();

        // Convert to XML
        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] bytes = converter.convertToXML(model);
        return new String(bytes);
    }

    private String getFormKeyForStage(String stageCode) {
        List<ScreenMapping> mappings = screenMappingRepository.findByStageCode(stageCode);
        return mappings.stream().findFirst().map(ScreenMapping::getScreenCode).orElse(stageCode);
    }

    private void addSlaTimer(Process process, UserTask userTask, BigDecimal days) {
        BoundaryEvent timer = new BoundaryEvent();
        timer.setId("timer_" + userTask.getId());
        timer.setAttachedToRef(userTask);
        timer.setCancelActivity(false); // Non-interrupting

        TimerEventDefinition timerDef = new TimerEventDefinition();
        long hours = days.multiply(BigDecimal.valueOf(24)).longValue();
        timerDef.setTimeDuration("PT" + hours + "H");
        timer.addEventDefinition(timerDef);

        process.addFlowElement(timer);

        // Connect timer to SLA Notification Service Task
        ServiceTask notificationTask = new ServiceTask();
        notificationTask.setId("slaNotification_" + userTask.getId());
        notificationTask.setName("SLA Notification");
        notificationTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        notificationTask.setImplementation("${slaNotificationService}");

        process.addFlowElement(notificationTask);

        // Sequence Flow from Timer to Notification
        SequenceFlow timerToNotify = new SequenceFlow();
        timerToNotify.setSourceRef(timer.getId());
        timerToNotify.setTargetRef(notificationTask.getId());
        process.addFlowElement(timerToNotify);

        // Sequence Flow from Notification to End (or separate end)
        EndEvent slaEnd = new EndEvent();
        slaEnd.setId("end_sla_" + userTask.getId());
        process.addFlowElement(slaEnd);

        SequenceFlow notifyToEnd = new SequenceFlow();
        notifyToEnd.setSourceRef(notificationTask.getId());
        notifyToEnd.setTargetRef(slaEnd.getId());
        process.addFlowElement(notifyToEnd);
    }

    private FlowableListener createListener(String className, String event) {
        FlowableListener listener = new FlowableListener();
        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        listener.setImplementation(className);
        listener.setEvent(event);
        return listener;
    }
}
