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
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.ImplementationType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

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

        // Sort stages by sequence
        // Sort stages by sequence
        List<StageConfig> sortedStages = new ArrayList<>(stages);
        sortedStages.sort((s1, s2) -> s1.getSequenceOrder().compareTo(s2.getSequenceOrder()));

        // Use sortedStages instead of stages below
        stages = sortedStages;

        // Create Valid Map of StageCode -> FlowElement
        Map<String, FlowElement> stageElements = new java.util.HashMap<>();

        // 1. Create All Stage Elements First (Nodes)
        for (StageConfig stage : stages) {
            FlowElement el = createStageElement(process, stage, workflow);
            process.addFlowElement(el);
            stageElements.put(stage.getStageCode(), el);

            // Add SLA behavior (Boundary Events) attached to this element
            addSlaIfConfigured(process, el, stage, workflow);
        }

        // 2. Connect Elements using Groups
        List<List<StageConfig>> groups = groupStages(stages);
        FlowElement previousElement = startEvent;

        for (int i = 0; i < groups.size(); i++) {
            List<StageConfig> currentGroup = groups.get(i);

            // Determine Next Group Entry for Lookahead
            FlowElement nextGroupEntry = null;
            StageConfig nextGroupFirstStage = null;

            if (i + 1 < groups.size()) {
                List<StageConfig> nextGroup = groups.get(i + 1);
                nextGroupFirstStage = nextGroup.get(0);
                if (nextGroup.size() > 1) {
                    // Next is Parallel, entry is Split Gateway (not created yet, but we need
                    // reference)
                    // We can create it now or predict ID?
                    // Better: Create Gateways just-in-time or create all upfront?
                    // Let's create Gateways inside the loop. But we need to connect TO it.
                    // So we must handle "Exit connects to Next Entry".
                    // OR: "Entry connects from Previous Exit". (Backwards)
                    // Let's do Backwards connecting.
                }
            }
        }

        // RESTART LOGIC - Backwards Connecting Model

        // Dictionary to hold Group Entry/Exit nodes for inter-group wiring
        // Map<GroupIndex, Pair<Entry, Exit>>

        // Pass 1: Create Group Gateways (Split/Join) so they exist
        List<FlowElement> groupEntryNodes = new ArrayList<>();
        List<FlowElement> groupExitNodes = new ArrayList<>();
        int parallelGatewayCounter = 1;

        for (List<StageConfig> group : groups) {
            if (group.size() > 1) {
                ParallelGateway split = new ParallelGateway();
                split.setId("split_" + parallelGatewayCounter);
                process.addFlowElement(split);
                groupEntryNodes.add(split);

                ParallelGateway join = new ParallelGateway();
                join.setId("join_" + parallelGatewayCounter);
                process.addFlowElement(join);
                groupExitNodes.add(join);

                parallelGatewayCounter++;
            } else {

                StageConfig s = group.get(0);
                FlowElement el = stageElements.get(s.getStageCode());
                groupEntryNodes.add(el);
                groupExitNodes.add(el); // Exit is same as Entry for Sequential (Flow logic handles internal branching)
            }
        }

        // End Event
        EndEvent end = new EndEvent();
        end.setId("end");
        process.addFlowElement(end);

        FlowElement lastNode = startEvent;

        for (int i = 0; i < groups.size(); i++) {
            List<StageConfig> group = groups.get(i);
            FlowElement entry = groupEntryNodes.get(i);
            FlowElement exit = groupExitNodes.get(i);
            FlowElement nextNode = (i + 1 < groups.size()) ? groupEntryNodes.get(i + 1) : end;

            // A. Incoming Connection (Last -> Entry)
            if (lastNode != null) {
                if (group.size() > 1) {
                    // Parallel: Connect Last -> Split directly.
                    // Individual Entry Conditions are handled INSIDE the parallel block.
                    connect(process, lastNode, entry);
                } else {
                    // Sequential: Connect Last -> Stage (Handle Entry Condition)
                    StageConfig stage = group.get(0);
                    FlowElement stageEl = entry;

                    // Note: If skipping, where do we go? Next Node.
                    if (stage.getEntryCondition() != null && !stage.getEntryCondition().isBlank()) {
                        handleEntryCondition(process, lastNode, stageEl, stage, nextNode);
                    } else {
                        connect(process, lastNode, stageEl);
                    }
                }
            }

            // B. Internal Processing & Outbound
            if (group.size() > 1) {
                // Parallel
                ParallelGateway split = (ParallelGateway) entry;
                ParallelGateway join = (ParallelGateway) exit;

                for (StageConfig stage : group) {
                    FlowElement stageEl = stageElements.get(stage.getStageCode());

                    // Split -> Stage (Handle Entry Condition)
                    if (stage.getEntryCondition() != null && !stage.getEntryCondition().isBlank()) {
                        handleEntryCondition(process, split, stageEl, stage, join); // Skip to Join
                    } else {
                        connect(process, split, stageEl);
                    }

                    // Stage -> Join (Handle Actions/Routing)
                    handleStageOutbound(process, stage, stageEl, join, stageElements, objectMapper);
                }

                // Join -> Next (Wait, Loop A of next iteration will connect Join -> Next entry)
                // So we do NOTHING here for outbound?
                // lastNode = join.
                lastNode = join;

            } else {
                // Sequential
                StageConfig stage = group.get(0);
                FlowElement stageEl = (FlowElement) entry;

                // Stage -> Next (Handle Actions/Routing)
                // Logic connects stageEl to nextNode.
                handleStageOutbound(process, stage, stageEl, nextNode, stageElements, objectMapper);

                // What is 'lastNode' for next iteration?
                // handleStageOutbound might wire to 'nextNode' via gateways.
                // But connections are made.
                // So Next Loop's "Connect Last -> Entry" (Step A) is REDUNDANT/DUPLICATE if we
                // use `lastNode=stageEl`.

                // If Sequential Stage connects to NextNode via handleStageOutbound,
                // the link is established.
                // Next iteration Step A will try to connect Last -> Entry.
                // If we set lastNode = null? Or skip Step A?

                // Alternative: handleStageOutbound does NOT wire the default flow?
                // No, it handles actions. Default Flow connects to "Next".

                // Solution:
                // Step A is only needed for Groups that need "Pre-Entry Connection" (like
                // Split).
                // Or if handleStageOutbound didn't cover it.

                // Let's modify:
                // Loop connects Current -> Next.
                // We don't rely on "Next Loop connecting Previous -> Current".
                //
                // So Step A is REMOVED.
                // Connect Start -> First Group (Before Loop).
                // Inside Loop:
                // Process Group -> Connects to NextNode.

                lastNode = null; // Unused concept in this approach
            }
        }

        // Loop Only Processes Outbound
        // ... (Proceed to implementation)

        // Auto Layout
        new BpmnAutoLayout(model).execute();

        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] bytes = converter.convertToXML(model);
        return new String(bytes);
    }

    // Helper to Group Stages
    private List<List<StageConfig>> groupStages(List<StageConfig> stages) {
        List<List<StageConfig>> groups = new ArrayList<>();
        if (stages.isEmpty())
            return groups;

        List<StageConfig> currentGroup = new ArrayList<>();
        currentGroup.add(stages.get(0));
        groups.add(currentGroup);

        for (int i = 1; i < stages.size(); i++) {
            StageConfig current = stages.get(i);
            StageConfig prev = stages.get(i - 1);

            boolean sameGroup = current.getParallelGrouping() != null
                    && !current.getParallelGrouping().isBlank()
                    && current.getParallelGrouping().equals(prev.getParallelGrouping());

            // Also check adjacency order? Already sorted.

            if (sameGroup) {
                currentGroup.add(current);
            } else {
                currentGroup = new ArrayList<>();
                currentGroup.add(current);
                groups.add(currentGroup);
            }
        }
        return groups;
    }

    // Extracted Logic for Stage Outbound
    private void handleStageOutbound(Process process, StageConfig currentStage, FlowElement source,
            FlowElement defaultTarget, Map<String, FlowElement> stageElements, ObjectMapper objectMapper) {
        // ... (Existing Logic refined)
        // Y.5 Exception Rules
        if (currentStage.getExceptionRules() != null && !currentStage.getExceptionRules().isBlank()) {
            // ...
        }

        // A. Routing Rules or Actions
        if ((currentStage.getRoutingRules() != null && !currentStage.getRoutingRules().isBlank())
                || (currentStage.getActions() != null && !currentStage.getActions().isEmpty())) {

            ExclusiveGateway gateway = new ExclusiveGateway();
            gateway.setId("gateway_split_" + currentStage.getStageCode() + "_" + System.nanoTime());
            process.addFlowElement(gateway);
            connect(process, source, gateway);

            // Process Rules/Actions (Connect Gateway -> Targets)

            // 1. Handle Configured Actions
            if (currentStage.getActions() != null) {
                for (com.workflow.service.entity.StageAction action : currentStage.getActions()) {
                    String targetType = action.getTargetType();
                    // Determine Target Element
                    FlowElement targetElement = null;

                    if ("SPECIFIC".equals(targetType)) {
                        String targetCode = action.getTargetStage();
                        if (targetCode != null && stageElements.containsKey(targetCode)) {
                            targetElement = stageElements.get(targetCode);
                        } else {
                            log.warn("Target stage {} not found for action {} in stage {}", targetCode,
                                    action.getActionLabel(), currentStage.getStageCode());
                        }
                    } else if ("END".equals(targetType)) {
                        EndEvent actionEnd = new EndEvent();
                        actionEnd.setId("end_action_" + currentStage.getStageCode() + "_" + action.getActionLabel());
                        process.addFlowElement(actionEnd);
                        targetElement = actionEnd;
                    } else if ("NEXT".equals(targetType)) {
                        // Next is defaultTarget passed to method
                        targetElement = defaultTarget;
                    }

                    if (targetElement != null) {
                        SequenceFlow actionFlow = connect(process, gateway, targetElement);
                        // Condition: ${outcome == 'LABEL'}
                        // Note: outcome variable is set in CaseService.completeTask
                        String condition = "${outcome == '" + action.getActionLabel() + "'}";
                        actionFlow.setConditionExpression(condition);
                    }
                }
            }

            // 2. Default Flow (If outcome doesn't match specific actions, or if no outcome)
            // Ideally we should have a fallback.
            // If defaultTarget exists, use it as default flow?
            // Or only if no condition met?

            if (defaultTarget != null) {
                SequenceFlow defFlow = connect(process, gateway, defaultTarget);
                gateway.setDefaultFlow(defFlow.getId());
            } else {
                // End
                EndEvent end = new EndEvent();
                end.setId("end_" + currentStage.getStageCode());
                process.addFlowElement(end);
                SequenceFlow defFlow = connect(process, gateway, end);
                gateway.setDefaultFlow(defFlow.getId());
            }
        } else {
            // Standard Sequential Flow
            if (defaultTarget != null) {
                connect(process, source, defaultTarget);
            } else {
                EndEvent end = new EndEvent();
                end.setId("end_" + currentStage.getStageCode());
                process.addFlowElement(end);
                connect(process, source, end);
            }
        }
    }

    private FlowElement createStageElement(Process process, StageConfig stage, WorkflowMaster workflow) {
        FlowElement stageElement;
        if (stage.isNestedWorkflow()) {
            CallActivity callActivity = new CallActivity();
            callActivity.setCalledElement(stage.getNestedWorkflowCode());
            callActivity.setInheritVariables(true);
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

            if (stage.getAssignmentRules() != null && !stage.getAssignmentRules().isBlank()) {
                try {
                    Map<String, Object> rules = objectMapper.readValue(stage.getAssignmentRules(),
                            new TypeReference<Map<String, Object>>() {
                            });
                    String mechanism = (String) rules.get("mechanism");

                    if ("GROUP_QUEUE".equals(mechanism) || "GROUP".equals(mechanism)) {
                        String group = (String) rules.get("groupName");
                        log.info("Generating BPMN: Stage {} configured for Group Queue: {}", stage.getStageCode(), group);
                        if (group != null)
                            userTask.setCandidateGroups(List.of(group));
                    } else if ("ROUND_ROBIN".equals(mechanism)) {
                        String pool = (String) rules.getOrDefault("roundRobinPool", rules.get("groupName"));
                        FlowableListener listener = new FlowableListener();
                        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                        listener.setImplementation("${roundRobinAssignmentListener}");
                        listener.setEvent("create");
                        FieldExtension poolField = new FieldExtension();
                        poolField.setFieldName("pool");
                        poolField.setStringValue(pool);
                        listener.setFieldExtensions(List.of(poolField));
                        userTask.setTaskListeners(new java.util.ArrayList<>(List.of(listener)));
                    } else if ("MATRIX_RULE".equals(mechanism) || "MATRIX".equals(mechanism)) {
                        FlowableListener listener = new FlowableListener();
                        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                        listener.setImplementation("${matrixAssignmentListener}");
                        listener.setEvent("create");
                        FieldExtension roleField = new FieldExtension();
                        roleField.setFieldName("role");
                        roleField.setStringValue((String) rules.get("matrixRole"));
                        listener.setFieldExtensions(List.of(roleField));
                        userTask.setTaskListeners(new java.util.ArrayList<>(List.of(listener)));
                    } else if ("MANUAL".equals(mechanism)) {
                        // US-1: Manual Assignment
                        // The functionality relies on a process variable being set by the previous task
                        // We use a standardized variable name: "manualAssignee"
                        // This variable is expected to contain the UserId of the assignee.
                        userTask.setAssignee("${manualAssignee}");
                    }
                } catch (Exception e) {
                    log.error("Failed to parse assignment rules for stage " + stage.getStageCode(), e);
                }
            }
            stageElement = userTask;
        }

        if (stage.isMultiInstance()) {
            MultiInstanceLoopCharacteristics loop = new MultiInstanceLoopCharacteristics();
            loop.setSequential(false); // Default to Parallel
            if (stage.getMiCollectionVariable() != null) {
                String collectionVar = stage.getMiCollectionVariable();
                if (!collectionVar.startsWith("${")) {
                    collectionVar = "${" + collectionVar + "}";
                }
                loop.setInputDataItem(collectionVar);
            }
            if (stage.getMiElementVariable() != null) {
                loop.setElementVariable(stage.getMiElementVariable());
            }
            if (stageElement instanceof Activity) {
                ((Activity) stageElement).setLoopCharacteristics(loop);
            }
        }

        stageElement.setId(stage.getStageCode());
        stageElement.setName(stage.getStageName());

        applyHooks(stageElement, stage);
        return stageElement;
    }

    private void applyHooks(FlowElement stageElement, StageConfig stage) {
        if (stage.getPreEntryHook() != null && !stage.getPreEntryHook().isBlank()) {
            if (stageElement.getExecutionListeners() == null)
                stageElement.setExecutionListeners(new ArrayList<>());
            stageElement.getExecutionListeners().add(createListener(stage.getPreEntryHook(), "start"));
        }
        if (stage.getPostExitHook() != null && !stage.getPostExitHook().isBlank()) {
            if (stageElement.getExecutionListeners() == null)
                stageElement.setExecutionListeners(new ArrayList<>());
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

    // handleEntryCondition Helper
    private void handleEntryCondition(Process process, FlowElement source, FlowElement target, StageConfig targetConfig,
            FlowElement nextAfterTarget) {
        ExclusiveGateway split = new ExclusiveGateway();
        split.setId("entry_split_" + targetConfig.getStageCode() + "_" + System.nanoTime());
        process.addFlowElement(split);

        connect(process, source, split);

        SequenceFlow enterFlow = connect(process, split, target);
        enterFlow.setConditionExpression(targetConfig.getEntryCondition());

        if (nextAfterTarget != null) {
            SequenceFlow skipFlow = connect(process, split, nextAfterTarget);
            split.setDefaultFlow(skipFlow.getId());
        } else {
            EndEvent end = new EndEvent();
            end.setId("end_skip_" + targetConfig.getStageCode());
            process.addFlowElement(end);
            SequenceFlow skipFlow = connect(process, split, end);
            split.setDefaultFlow(skipFlow.getId());
        }
    }

    private SequenceFlow connect(Process process, FlowElement source, FlowElement target) {
        SequenceFlow flow = new SequenceFlow();
        flow.setId("flow_" + source.getId() + "_" + target.getId() + "_" + System.nanoTime()); // Unique ID
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
        timer.setCancelActivity(false);

        TimerEventDefinition timerDef = new TimerEventDefinition();
        long hours = days.multiply(BigDecimal.valueOf(24)).longValue();
        timerDef.setTimeDuration("PT" + hours + "H");
        timer.addEventDefinition(timerDef);

        process.addFlowElement(timer);

        ServiceTask notificationTask = new ServiceTask();
        notificationTask.setId("slaNotification_" + userTask.getId());
        notificationTask.setName("SLA Notification");
        notificationTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        notificationTask.setImplementation("${slaNotificationService}");

        process.addFlowElement(notificationTask);

        connect(process, timer, notificationTask);

        EndEvent slaEnd = new EndEvent();
        slaEnd.setId("end_sla_" + userTask.getId());
        process.addFlowElement(slaEnd);

        connect(process, notificationTask, slaEnd);
    }

    private FlowableListener createListener(String className, String event) {
        FlowableListener listener = new FlowableListener();
        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        listener.setImplementation(className);
        listener.setEvent(event);
        return listener;
    }
}
