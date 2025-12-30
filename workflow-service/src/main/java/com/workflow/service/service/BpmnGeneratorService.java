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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class BpmnGeneratorService {

    private final ScreenMappingRepository screenMappingRepository;
    private final ObjectMapper objectMapper;

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

        // Sort stages by sequence
        stages.sort((s1, s2) -> s1.getSequenceOrder().compareTo(s2.getSequenceOrder()));

        // Group stages by Sequence Order + Parallel Grouping
        List<List<StageConfig>> groupedStages = groupStages(stages);

        for (List<StageConfig> group : groupedStages) {
            if (group.isEmpty())
                continue;

            if (group.size() == 1) {
                // Single Stage (Standard Flow)
                StageConfig stage = group.get(0);
                FlowElement stageElement = createStageElement(process, stage, workflow);
                process.addFlowElement(stageElement);

                // 1. Entry Condition (Skip Logic)
                FlowElement exitPoint = stageElement;

                if (stage.getEntryCondition() != null && !stage.getEntryCondition().isBlank()) {
                    ExclusiveGateway split = new ExclusiveGateway();
                    split.setId("entry_split_" + stage.getStageCode());
                    process.addFlowElement(split);

                    ExclusiveGateway join = new ExclusiveGateway();
                    join.setId("entry_join_" + stage.getStageCode());
                    process.addFlowElement(join);

                    // Connect Previous -> Split
                    connect(process, previousElement, split);

                    // Split -> Stage (Condition Met)
                    SequenceFlow enterFlow = connect(process, split, stageElement);
                    enterFlow.setConditionExpression(stage.getEntryCondition());

                    // Split -> Join (Skip)
                    SequenceFlow skipFlow = connect(process, split, join);
                    // This skip flow acts as the path if condition is false.
                    // To be explicit, we could rely on default flow behavior or inverse condition.
                    // For now, Flowable handles standard condition eval. If one has condition and
                    // other doesn't,
                    // the one without condition acts as default/fallback usually.
                    split.setDefaultFlow(skipFlow.getId());

                    // Stage -> Join
                    connect(process, stageElement, join);

                    exitPoint = join;
                } else {
                    // Standard Connect
                    connect(process, previousElement, stageElement);
                }

                // Handle SLA
                addSlaIfConfigured(process, stageElement, stage, workflow);

                // 2. Action Routing (Post-Stage Branching)
                if (stage.getActions() != null && !stage.getActions().isEmpty()) {
                    // Check if any action needs routing
                    boolean hasRouting = stage.getActions().stream()
                            .anyMatch(a -> "SPECIFIC".equals(a.getTargetType()) || "END".equals(a.getTargetType()));

                    if (hasRouting) {
                        ExclusiveGateway actionSplit = new ExclusiveGateway();
                        actionSplit.setId("action_split_" + stage.getStageCode());
                        process.addFlowElement(actionSplit);

                        connect(process, exitPoint, actionSplit);
                        
                        // Create flows for routed actions
                        for (com.workflow.service.entity.StageAction action : stage.getActions()) {
                            String targetType = action.getTargetType();
                            String label = action.getActionLabel(); 
                            
                            if ("SPECIFIC".equals(targetType)) {
                                String targetStageCode = action.getTargetStage();
                                if(targetStageCode != null) {
                                    SequenceFlow flow = new SequenceFlow();
                                    flow.setSourceRef(actionSplit.getId());
                                    flow.setTargetRef(targetStageCode);
                                    flow.setConditionExpression("${outcome == '" + label + "'}");
                                    process.addFlowElement(flow);
                                }
                            } else if ("END".equals(targetType)) {
                                EndEvent end = new EndEvent();
                                end.setId("end_" + stage.getStageCode() + "_" + label);
                                process.addFlowElement(end);

                                SequenceFlow flow = connect(process, actionSplit, end);
                                flow.setConditionExpression("${outcome == '" + label + "'}");
                            }
                        }
                        
                        exitPoint = actionSplit;
                    }
                }

                previousElement = exitPoint;
            } else {
                // Parallel Block
                ParallelGateway split = new ParallelGateway();
                split.setId("split_" + group.get(0).getSequenceOrder());
                process.addFlowElement(split);
                connect(process, previousElement, split);

                ParallelGateway join = new ParallelGateway();
                join.setId("join_" + group.get(0).getSequenceOrder());
                process.addFlowElement(join);

                for (StageConfig stage : group) {
                    FlowElement stageElement = createStageElement(process, stage, workflow);
                    process.addFlowElement(stageElement);

                    connect(process, split, stageElement);
                    connect(process, stageElement, join);

                    addSlaIfConfigured(process, stageElement, stage, workflow);
                }

                previousElement = join;
            }
        }

        // Completion Mapping
        if (workflow.getCompletionApiEndpoint() != null && !workflow.getCompletionApiEndpoint().isBlank()) {
            ServiceTask completionTask = new ServiceTask();
            completionTask.setId("completionServiceTask");
            completionTask.setName("Call Completion API");
            completionTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            completionTask.setImplementation("${completionService}");

            process.addFlowElement(completionTask);
            connect(process, previousElement, completionTask);
            previousElement = completionTask;
        }

        // End Event
        EndEvent endEvent = new EndEvent();
        endEvent.setId("end");
        process.addFlowElement(endEvent);
        connect(process, previousElement, endEvent);

        // Auto Layout
        new BpmnAutoLayout(model).execute();

        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] bytes = converter.convertToXML(model);
        return new String(bytes);
    }

    private List<List<StageConfig>> groupStages(List<StageConfig> stages) {
        // Group by Sequence ID. If 'parallelGrouping' is set, verify consistency.
        // Simple Logic: Iterate and group by sequence.
        List<List<StageConfig>> groups = new java.util.ArrayList<>();
        if (stages.isEmpty())
            return groups;

        java.util.Map<Integer, List<StageConfig>> map = new java.util.TreeMap<>(); // Sorted by sequence
        for (StageConfig s : stages) {
            map.computeIfAbsent(s.getSequenceOrder(), k -> new java.util.ArrayList<>()).add(s);
        }
        return new java.util.ArrayList<>(map.values());
    }

    private FlowElement createStageElement(Process process, StageConfig stage, WorkflowMaster workflow) {
        FlowElement stageElement;
        if (stage.isNestedWorkflow()) {
            CallActivity callActivity = new CallActivity();
            callActivity.setCalledElement(stage.getNestedWorkflowCode());
            stageElement = callActivity;
        } else if (stage.isRuleStage()) {
            ServiceTask ruleTask = new ServiceTask();
            ruleTask.setType("dmn");
            FieldExtension prevField = new FieldExtension();
            prevField.setFieldName("decisionTableReferenceKey");
            prevField.setStringValue(stage.getRuleKey());
            ruleTask.getFieldExtensions().add(prevField);
            stageElement = ruleTask;
        } else {
            UserTask userTask = new UserTask();
            String formKey = getFormKeyForStage(stage.getStageCode());
            userTask.setFormKey(formKey);
            stageElement = userTask;
        }

        stageElement.setId(stage.getStageCode());
        stageElement.setName(stage.getStageName());

        applyHooks(stageElement, stage);
        return stageElement;
    }

    private void applyHooks(FlowElement stageElement, StageConfig stage) {
        if (stage.getPreEntryHook() != null && !stage.getPreEntryHook().isBlank()) {
            stageElement.setExecutionListeners(List.of(createListener(stage.getPreEntryHook(), "start")));
        }
        if (stage.getPostExitHook() != null && !stage.getPostExitHook().isBlank()) {
            stageElement.getExecutionListeners().add(createListener(stage.getPostExitHook(), "end"));
        }

        if (stageElement instanceof UserTask) {
            UserTask userTask = (UserTask) stageElement;
            if (stage.getPostEntryHook() != null && !stage.getPostEntryHook().isBlank()) {
                userTask.getTaskListeners().add(createListener(stage.getPostEntryHook(), "create"));
            }
            if (stage.getPreExitHook() != null && !stage.getPreExitHook().isBlank()) {
                userTask.getTaskListeners().add(createListener(stage.getPreExitHook(), "complete"));
            }
        }
    }

    private SequenceFlow connect(Process process, FlowElement source, FlowElement target) {
        SequenceFlow flow = new SequenceFlow();
        flow.setSourceRef(source.getId());
        flow.setTargetRef(target.getId());
        process.addFlowElement(flow);
        return flow;
    }

    private void addSlaIfConfigured(Process process, FlowElement stageElement, StageConfig stage,
            WorkflowMaster workflow) {
        BigDecimal slaDays = stage.getSlaDurationDays();
        if (slaDays == null || slaDays.compareTo(BigDecimal.ZERO) <= 0) {
            if (workflow.getSlaDurationDays() != null && workflow.getSlaDurationDays().compareTo(BigDecimal.ZERO) > 0) {
                slaDays = workflow.getSlaDurationDays();
            }
        }
        if (slaDays != null && slaDays.compareTo(BigDecimal.ZERO) > 0) {
            if (stageElement instanceof UserTask) {
                addSlaTimer(process, (UserTask) stageElement, slaDays);
            }
        }
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
