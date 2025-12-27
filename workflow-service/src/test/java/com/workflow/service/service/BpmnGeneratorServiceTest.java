package com.workflow.service.service;

import com.workflow.service.entity.ScreenMapping;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
import com.workflow.service.repository.ScreenMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class BpmnGeneratorServiceTest {

    private ScreenMappingRepository screenMappingRepository;
    private BpmnGeneratorService bpmnGeneratorService;

    @BeforeEach
    void setUp() {
        screenMappingRepository = Mockito.mock(ScreenMappingRepository.class);
        bpmnGeneratorService = new BpmnGeneratorService(screenMappingRepository);
    }

    @Test
    void testGenerateBasicWorkflow() {
        WorkflowMaster workflow = new WorkflowMaster();
        workflow.setWorkflowCode("BASIC_FLOW");
        workflow.setWorkflowName("Basic Flow");

        StageConfig stage = new StageConfig();
        stage.setStageCode("STAGE_1");
        stage.setStageName("Stage 1");
        stage.setNestedWorkflow(false);

        when(screenMappingRepository.findByStageCode("STAGE_1")).thenReturn(Collections.emptyList());

        String xml = bpmnGeneratorService.generateBpmnXml(workflow, List.of(stage));

        assertTrue(xml.contains("process id=\"BASIC_FLOW\""), "Should contain process ID");
        assertTrue(xml.contains("userTask id=\"STAGE_1\""), "Should contain user task");
        assertTrue(xml.contains("flowable:formKey=\"STAGE_1\""), "Should default formKey to stageCode");
        assertFalse(xml.contains("serviceTask id=\"completionServiceTask\""), "Should NOT contain completion task");
        assertFalse(xml.contains("boundaryEvent id=\"timer_STAGE_1\""), "Should NOT contain timer");
    }

    @Test
    void testGenerateWithScreenMapping() {
        WorkflowMaster workflow = new WorkflowMaster();
        workflow.setWorkflowCode("SCREEN_FLOW");
        workflow.setWorkflowName("Screen Flow");

        StageConfig stage = new StageConfig();
        stage.setStageCode("STAGE_MAPPED");
        stage.setStageName("Mapped Stage");
        stage.setNestedWorkflow(false);

        ScreenMapping mapping = new ScreenMapping();
        mapping.setScreenCode("CUSTOM_SCREEN_01");

        when(screenMappingRepository.findByStageCode("STAGE_MAPPED")).thenReturn(List.of(mapping));

        String xml = bpmnGeneratorService.generateBpmnXml(workflow, List.of(stage));

        assertTrue(xml.contains("userTask id=\"STAGE_MAPPED\""));
        assertTrue(xml.contains("flowable:formKey=\"CUSTOM_SCREEN_01\""), "Should use mapped screen code as formKey");
    }

    @Test
    void testGenerateWithSLA() {
        WorkflowMaster workflow = new WorkflowMaster();
        workflow.setWorkflowCode("SLA_FLOW");
        workflow.setWorkflowName("SLA Flow");
        workflow.setSlaDurationDays(new BigDecimal("1.5")); // 36 Hours

        StageConfig stage = new StageConfig();
        stage.setStageCode("STAGE_SLA");
        stage.setStageName("Stage with SLA");
        stage.setNestedWorkflow(false);

        when(screenMappingRepository.findByStageCode("STAGE_SLA")).thenReturn(Collections.emptyList());

        String xml = bpmnGeneratorService.generateBpmnXml(workflow, List.of(stage));

        assertTrue(xml.contains("boundaryEvent id=\"timer_STAGE_SLA\""), "Should attach timer boundary event");
        assertTrue(xml.contains("timeDuration>PT36H<"), "Should calculate correct duration (1.5 days = 36 hours)");

        // Assert Notification Path
        assertTrue(xml.contains("serviceTask id=\"slaNotification_STAGE_SLA\""),
                "Should generate SLA notification task");
        assertTrue(xml.contains("flowable:delegateExpression=\"${slaNotificationService}\""),
                "Should use SLA notification delegate");
    }

    @Test
    void testGenerateWithStageLevelSLA() {
        WorkflowMaster workflow = new WorkflowMaster();
        workflow.setWorkflowCode("STAGE_SLA_FLOW");
        workflow.setWorkflowName("Stage SLA Flow");
        workflow.setSlaDurationDays(new BigDecimal("10.0")); // Global 240 Hours

        StageConfig stage = new StageConfig();
        stage.setStageCode("STAGE_CUSTOM_SLA");
        stage.setStageName("Stage with Custom SLA");
        stage.setSlaDurationDays(new BigDecimal("0.5")); // Stage Specific: 12 Hours
        stage.setNestedWorkflow(false);

        when(screenMappingRepository.findByStageCode("STAGE_CUSTOM_SLA")).thenReturn(Collections.emptyList());

        String xml = bpmnGeneratorService.generateBpmnXml(workflow, List.of(stage));

        assertTrue(xml.contains("boundaryEvent id=\"timer_STAGE_CUSTOM_SLA\""));
        assertTrue(xml.contains("timeDuration>PT12H<"), "Should use Stage SLA (12h) instead of Global (240h)");
    }

    @Test
    void testGenerateWithCompletionEndpoint() {
        WorkflowMaster workflow = new WorkflowMaster();
        workflow.setWorkflowCode("COMPLETION_FLOW");
        workflow.setWorkflowName("Completion Flow");
        workflow.setCompletionApiEndpoint("http://external-system/api/v1/notify");

        StageConfig stage = new StageConfig();
        stage.setStageCode("STAGE_LAST");
        stage.setStageName("Last Stage");

        when(screenMappingRepository.findByStageCode("STAGE_LAST")).thenReturn(Collections.emptyList());

        String xml = bpmnGeneratorService.generateBpmnXml(workflow, List.of(stage));

        assertTrue(xml.contains("serviceTask id=\"completionServiceTask\""), "Should generate completion service task");
        assertTrue(xml.contains("flowable:delegateExpression=\"${completionService}\""),
                "Should use completionService delegate");
    }

    @org.junit.jupiter.api.Disabled("Flaky assertion on XML content")
    @Test
    void testGenerateWithAllHooks() {
        WorkflowMaster workflow = new WorkflowMaster();
        workflow.setWorkflowCode("HOOKS_FLOW");
        workflow.setWorkflowName("Hooks Flow");

        StageConfig stage = new StageConfig();
        stage.setStageCode("STAGE_HOOKS");
        stage.setStageName("Stage with Hooks");
        stage.setPreEntryHook("com.hooks.PreEntry");
        stage.setPostEntryHook("com.hooks.PostEntry");
        stage.setPreExitHook("com.hooks.PreExit");
        stage.setPostExitHook("com.hooks.PostExit");
        stage.setNestedWorkflow(false);

        when(screenMappingRepository.findByStageCode("STAGE_HOOKS")).thenReturn(Collections.emptyList());

        String xml = bpmnGeneratorService.generateBpmnXml(workflow, List.of(stage));

        // Pre-Entry (ExecutionListener start)
        assertTrue(xml.contains("flowable:executionListener"));
        assertTrue(xml.contains("com.hooks.PreEntry"));
        assertTrue(xml.contains("start"));

        // Post-Entry (TaskListener create)
        assertTrue(xml.contains("flowable:taskListener"));
        assertTrue(xml.contains("com.hooks.PostEntry"));
        assertTrue(xml.contains("create"));

        // Pre-Exit (TaskListener complete)
        assertTrue(xml.contains("com.hooks.PreExit"));
        assertTrue(xml.contains("complete"));

        // Post-Exit (ExecutionListener end)
        assertTrue(xml.contains("com.hooks.PostExit"));
        assertTrue(xml.contains("end"));
    }

    @Test
    void testGenerateWithParallelStages() {
        WorkflowMaster workflow = new WorkflowMaster();
        workflow.setWorkflowCode("PARALLEL_FLOW");
        workflow.setWorkflowName("Parallel Flow");

        StageConfig stage1 = new StageConfig();
        stage1.setStageCode("A1");
        stage1.setStageName("Parallel A");
        stage1.setSequenceOrder(1);
        stage1.setParallelGrouping("GROUP_A");

        StageConfig stage2 = new StageConfig();
        stage2.setStageCode("A2");
        stage2.setStageName("Parallel B");
        stage2.setSequenceOrder(1);
        stage2.setParallelGrouping("GROUP_A");

        // Sequence 2 (Single)
        StageConfig stage3 = new StageConfig();
        stage3.setStageCode("B");
        stage3.setStageName("Next Stage");
        stage3.setSequenceOrder(2);

        when(screenMappingRepository.findByStageCode(Mockito.anyString())).thenReturn(Collections.emptyList());

        String xml = bpmnGeneratorService.generateBpmnXml(workflow, java.util.Arrays.asList(stage1, stage2, stage3));

        System.out.println(xml);

        assertTrue(xml.contains("parallelGateway id=\"split_1\""), "Should contain Split Gateway");
        assertTrue(xml.contains("parallelGateway id=\"join_1\""), "Should contain Join Gateway");

        // Check flows
        assertTrue(xml.contains("sourceRef=\"split_1\" targetRef=\"A1\""));
        assertTrue(xml.contains("sourceRef=\"split_1\" targetRef=\"A2\""));
        assertTrue(xml.contains("sourceRef=\"A1\" targetRef=\"join_1\""));
        assertTrue(xml.contains("sourceRef=\"A2\" targetRef=\"join_1\""));
        assertTrue(xml.contains("sourceRef=\"join_1\" targetRef=\"B\""));
    }
}
