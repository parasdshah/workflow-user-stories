package com.workflow.service.controller;

import com.workflow.service.service.RuntimeStatusService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/runtime")
@RequiredArgsConstructor
public class RuntimeController {

    private final RuntimeStatusService runtimeStatusService;

    @GetMapping("/cases/{caseId}/status")
    public ResponseEntity<String> getCaseStatus(@PathVariable String caseId) {
        ProcessInstance instance = runtimeStatusService.getCaseStatus(caseId);
        if (instance == null) {
            // Check history or return not found
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(instance.isEnded() ? "COMPLETED" : "ACTIVE");
    }

    @GetMapping("/cases")
    public ResponseEntity<List<CaseDto>> getAllCases() {
        // List all active cases
        List<ProcessInstance> instances = runtimeStatusService.getAllCases();
        List<CaseDto> dtos = instances.stream()
                .map(i -> new CaseDto(i.getId(), i.getProcessDefinitionKey(), i.getStartTime()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    record CaseDto(String id, String workflowCode, java.util.Date startTime) {
    }
}
