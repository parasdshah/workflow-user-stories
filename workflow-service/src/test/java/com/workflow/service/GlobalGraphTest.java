package com.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.service.dto.GraphDTO;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
import com.workflow.service.repository.StageConfigRepository;
import com.workflow.service.repository.WorkflowMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GlobalGraphTest {

    @Autowired
    private MockMvc mockMvc;

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
    void testGetGlobalGraph_WithNestedWorkflow() throws Exception {
        // 1. Create Child Workflow (SUB)
        WorkflowMaster subWf = new WorkflowMaster();
        subWf.setWorkflowCode("SUB");
        subWf.setWorkflowName("Sub Workflow");
        workflowRepository.save(subWf);

        StageConfig subStage1 = new StageConfig();
        subStage1.setWorkflowCode("SUB");
        subStage1.setStageCode("SUB_S1");
        subStage1.setStageName("Sub Stage 1");
        subStage1.setSequenceOrder(1);
        stageRepository.save(subStage1);

        // 2. Create Parent Workflow (MAIN)
        WorkflowMaster mainWf = new WorkflowMaster();
        mainWf.setWorkflowCode("MAIN");
        mainWf.setWorkflowName("Main Workflow");
        workflowRepository.save(mainWf);

        StageConfig mainStage1 = new StageConfig();
        mainStage1.setWorkflowCode("MAIN");
        mainStage1.setStageCode("MAIN_S1");
        mainStage1.setStageName("Main Stage 1");
        mainStage1.setSequenceOrder(1);
        stageRepository.save(mainStage1);

        StageConfig nestedStage = new StageConfig();
        nestedStage.setWorkflowCode("MAIN");
        nestedStage.setStageCode("NESTED_CALL");
        nestedStage.setStageName("Call Sub");
        nestedStage.setSequenceOrder(2);
        nestedStage.setNestedWorkflow(true);
        nestedStage.setNestedWorkflowCode("SUB");
        stageRepository.save(nestedStage);

        StageConfig mainStage2 = new StageConfig();
        mainStage2.setWorkflowCode("MAIN");
        mainStage2.setStageCode("MAIN_S2");
        mainStage2.setStageName("Main Stage 2");
        mainStage2.setSequenceOrder(3);
        stageRepository.save(mainStage2);

        // 3. Call API
        MvcResult result = mockMvc.perform(get("/api/workflows/MAIN/global-graph"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        GraphDTO graph = objectMapper.readValue(json, GraphDTO.class);

        // 4. Verify
        assertThat(graph).isNotNull();
        assertThat(graph.getNodes()).hasSizeGreaterThan(3);
        // Expected nodes:
        // MAIN start, MAIN end, MAIN_S1, NESTED_CALL (Group), MAIN_S2
        // Inside NESTED_CALL: SUB start, SUB end, SUB_S1
        // Total approx 8 nodes.

        // Check for Group Node
        boolean hasGroup = graph.getNodes().stream()
                .anyMatch(n -> n.getType().equals("bpmnGroup") && n.getData() instanceof java.util.Map
                        && "NESTED_CALL".equals(((java.util.Map) n.getData()).get("stageCode")));
        assertThat(hasGroup).isTrue();

        // Check for Child Node with ParentId
        boolean hasChild = graph.getNodes().stream()
                .anyMatch(n -> n.getParentId() != null && n.getParentId().contains("NESTED_CALL"));
        assertThat(hasChild).isTrue();
    }
}
