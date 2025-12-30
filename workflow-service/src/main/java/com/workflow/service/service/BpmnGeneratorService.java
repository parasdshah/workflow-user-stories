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

        FlowElement lastElement = startEvent;

        // Sort stages by sequence
        stages.sort((s1, s2) -> s1.getSequenceOrder().compareTo(s2.getSequenceOrder()));

        // Create Valid Map of StageCode -> FlowElement (UserTask/ServiceTask/CallActivity)
        Map<String, FlowElement> stageElements = new java.util.HashMap<>();
        
        // 1. Create All Stage Elements First (Nodes)
        for (StageConfig stage : stages) {
            FlowElement el = createStageElement(process, stage, workflow);
            process.addFlowElement(el);
            stageElements.put(stage.getStageCode(), el);
            
            // Add SLA behavior (Boundary Events) attached to this element
            addSlaIfConfigured(process, el, stage, workflow);
        }

        // 2. Connect Elements (Edges)
        // We iterate through the sorted list to establish default flows
        for (int i = 0; i < stages.size(); i++) {
            StageConfig currentStage = stages.get(i);
            FlowElement currentElement = stageElements.get(currentStage.getStageCode());
            
            // Connect Predecessor -> Current (Only if strictly sequential and not already targeted by jump)
            // Actually, for proper graph, we handle "Outbound" connections from Previous to Current
            // Logic: The "Start" connects to the first stage.
            if (i == 0) {
                // Connect Start -> First Stage
                if (currentStage.getEntryCondition() != null && !currentStage.getEntryCondition().isBlank()) {
                     // Entry Condition Logic for First Stage
                     // Start -> Split -> (Cond) -> FirstStage
                     //                -> (Def) -> End (Skip) or Next?
                     // Simplification: Entry Conditions usually strictly skip *this* stage to *next* stage.
                     // But if it's the first stage, skipping might mean ending? Or going to 2nd?
                     handleEntryCondition(process, startEvent, currentElement, currentStage, getNextStageElement(stages, i, stageElements));
                } else {
                     connect(process, startEvent, currentElement);
                }
            }
            
            // Determine Outbound Flows from Current Stage
            FlowElement source = currentElement;
            FlowElement defaultTarget = getNextStageElement(stages, i, stageElements);

            // A. Routing Rules (Branching)
            if (currentStage.getRoutingRules() != null && !currentStage.getRoutingRules().isBlank()) {
                // Add Exclusive Gateway for Branching
                ExclusiveGateway gateway = new ExclusiveGateway();
                gateway.setId("gateway_split_" + currentStage.getStageCode());
                process.addFlowElement(gateway);
                
                // Connect Stage -> Gateway
                connect(process, source, gateway);
                
                try {
                    List<Map<String, String>> rules = objectMapper.readValue(currentStage.getRoutingRules(), new TypeReference<List<Map<String, String>>>(){});
                    boolean hasDefault = false;
                    
                    for (Map<String, String> rule : rules) {
                        String condition = rule.get("condition");
                        String targetCode = rule.get("targetStageCode");
                        FlowElement targetEl = stageElements.get(targetCode);
                        
                        if (targetEl != null) {
                            SequenceFlow flow = connect(process, gateway, targetEl);
                            if (condition != null && !condition.isBlank()) {
                                flow.setConditionExpression(condition);
                            } else {
                                // Empty condition might imply default flow in UI config? 
                                // Or we treat it as specific "Always" path?
                                // Standard: Condition required.
                            }
                        }
                    }
                    
                    // Default Flow (if no condition met) -> Next Sequential Stage
                    if (defaultTarget != null) {
                        SequenceFlow defFlow = connect(process, gateway, defaultTarget);
                        gateway.setDefaultFlow(defFlow.getId());
                    } else {
                        // End of workflow
                         EndEvent end = new EndEvent();
                         end.setId("end_" + currentStage.getStageCode());
                         process.addFlowElement(end);
                         SequenceFlow defFlow = connect(process, gateway, end);
                         gateway.setDefaultFlow(defFlow.getId());
                    }

                } catch(Exception e) {
                   log.error("Failed to parse routing rules for stage " + currentStage.getStageCode(), e);
                   // Fallback to strict sequence
                   if (defaultTarget != null) connect(process, source, defaultTarget);
                }

            } else {
                // B. Standard Sequential Flow
                // Check if Next Stage has Entry Condition (Skip Logic)
                if (defaultTarget != null) {
                     StageConfig nextStage = stages.get(i+1); // Safe because defaultTarget not null implies i+1 exists
                     if (nextStage.getEntryCondition() != null && !nextStage.getEntryCondition().isBlank()) {
                         // Connect Source -> [EntryLogic] -> Target
                         handleEntryCondition(process, source, defaultTarget, nextStage, getNextStageElement(stages, i+1, stageElements));
                     } else {
                         connect(process, source, defaultTarget);
                     }
                } else {
                    // No next stage -> End
                    EndEvent end = new EndEvent();
                    end.setId("end");
                    process.addFlowElement(end);
                    connect(process, source, end);
                }
            }
        }

        // Auto Layout
        new BpmnAutoLayout(model).execute();

        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] bytes = converter.convertToXML(model);
        return new String(bytes);
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

    private FlowElement getNextStageElement(List<StageConfig> stages, int currentIndex, Map<String, FlowElement> elementMap) {
        if (currentIndex + 1 < stages.size()) {
            return elementMap.get(stages.get(currentIndex + 1).getStageCode());
        }
        return null;
    }

    private void handleEntryCondition(Process process, FlowElement source, FlowElement target, StageConfig targetConfig, FlowElement nextAfterTarget) {
            ExclusiveGateway split = new ExclusiveGateway();
            split.setId("entry_split_" + targetConfig.getStageCode());
            process.addFlowElement(split);

            ExclusiveGateway join = new ExclusiveGateway(); // Or simply connect to next?
            // "Skip" means go to nextAfterTarget.
            
            // Connect Source -> Split
            connect(process, source, split);
            
            // Split -> Target (Condition Met)
            SequenceFlow enterFlow = connect(process, split, target);
            enterFlow.setConditionExpression(targetConfig.getEntryCondition());
            
            // Split -> Skip (Default)
            if (nextAfterTarget != null) {
                 SequenceFlow skipFlow = connect(process, split, nextAfterTarget);
                 split.setDefaultFlow(skipFlow.getId());
            } else {
                // Skip to End
                EndEvent end = new EndEvent();
                end.setId("end_skip_" + targetConfig.getStageCode());
                process.addFlowElement(end);
                SequenceFlow skipFlow = connect(process, split, end);
                split.setDefaultFlow(skipFlow.getId());
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
