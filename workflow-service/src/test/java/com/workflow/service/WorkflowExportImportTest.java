package com.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
import com.workflow.service.repository.StageConfigRepository;
import com.workflow.service.repository.WorkflowMasterRepository;
import com.workflow.service.service.WorkflowExportImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class WorkflowExportImportTest {

    @Autowired
    private WorkflowExportImportService exportImportService;

    @Autowired
    private WorkflowMasterRepository workflowRepository;

    @Autowired
    private StageConfigRepository stageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        workflowRepository.deleteAll();
        stageRepository.deleteAll();
    }

    @Test
    void testExportImport_WithReworkConfiguration() throws Exception {
        // 1. Create Workflow with Rework Exception Rules
        WorkflowMaster wf = new WorkflowMaster();
        wf.setWorkflowCode("REWORK_WF");
        wf.setWorkflowName("Rework Workflow");
        workflowRepository.save(wf);

        StageConfig stage = new StageConfig();
        stage.setWorkflowCode("REWORK_WF");
        stage.setStageCode("S1");
        stage.setStageName("Stage 1");
        stage.setSequenceOrder(1);

        // Add Exception Rules (JSON String)
        String rulesJson = "[{\"errorCode\":\"REWORK_REQUIRED\",\"targetStageCode\":\"S1\"}]";
        stage.setExceptionRules(rulesJson);

        stageRepository.save(stage);

        // 2. Export
        byte[] jsonData = exportImportService.exportWorkflow("REWORK_WF", false);
        assertThat(jsonData).isNotEmpty();

        // 3. Delete Original
        stageRepository.deleteAll();
        workflowRepository.deleteAll();

        // 4. Import
        org.springframework.mock.web.MockMultipartFile jsonFile = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "workflow.json",
                "application/json",
                jsonData);
        exportImportService.importWorkflow(jsonFile);

        // 5. Verify
        WorkflowMaster importedWf = workflowRepository.findByWorkflowCode("REWORK_WF").orElseThrow();
        List<StageConfig> importedStages = stageRepository.findByWorkflowCodeOrderBySequenceOrderAsc("REWORK_WF");

        assertThat(importedStages).hasSize(1);
        StageConfig importedStage = importedStages.get(0);

        assertThat(importedStage.getExceptionRules()).isEqualTo(rulesJson);

        // Also verify parsing
        List<Map<String, Object>> parsedRules = objectMapper.readValue(importedStage.getExceptionRules(), List.class);
        assertThat(parsedRules).hasSize(1);
        assertThat(parsedRules.get(0).get("errorCode")).isEqualTo("REWORK_REQUIRED");
    }
}
