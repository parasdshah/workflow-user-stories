package com.workflow.service.controller;

import com.workflow.service.dto.CaseDTO;
import com.workflow.service.dto.StageDTO;
import com.workflow.service.service.CaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaseController.class)
public class CaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CaseService caseService;

    @Test
    void testInitiateCase() throws Exception {
        when(caseService.initiateCase(anyString(), anyMap(), anyString())).thenReturn("DIS-123");

        String payload = "{\"workflowCode\": \"LOAN_PROCESS\", \"userId\": \"john\", \"variables\": {}}";

        mockMvc.perform(post("/api/runtime/cases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("DIS-123"));
    }

    @Test
    void testGetCaseDetails() throws Exception {
        CaseDTO dto = new CaseDTO();
        dto.setCaseId("DIS-123");
        dto.setWorkflowCode("LOAN_PROCESS");
        dto.setStatus("ACTIVE");

        when(caseService.getCaseDetails("DIS-123")).thenReturn(dto);

        mockMvc.perform(get("/api/runtime/cases/DIS-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value("DIS-123"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testGetCaseDetailsNotFound() throws Exception {
        when(caseService.getCaseDetails("UNKNOWN")).thenThrow(new IllegalArgumentException("Case not found"));

        mockMvc.perform(get("/api/runtime/cases/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCaseStages() throws Exception {
        StageDTO stage = new StageDTO();
        stage.setStageName("Review");
        stage.setStatus("ACTIVE");

        when(caseService.getStages("DIS-123")).thenReturn(List.of(stage));

        mockMvc.perform(get("/api/runtime/cases/DIS-123/stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageName").value("Review"));
    }
}
