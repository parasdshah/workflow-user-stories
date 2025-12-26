package com.workflow.service.controller;

import com.workflow.service.dto.CaseDTO;
import com.workflow.service.dto.InitiateCaseRequest;
import com.workflow.service.dto.StageDTO;
import com.workflow.service.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/runtime/cases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    public ResponseEntity<String> initiateCase(@RequestBody InitiateCaseRequest request) {
        String workflowCode = request.getWorkflowCode();
        String userId = request.getUserId() != null ? request.getUserId() : "anonymous";
        Map<String, Object> variables = request.getVariables();

        try {
            String caseId = caseService.initiateCase(workflowCode, variables, userId);
            return ResponseEntity.ok(caseId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error starting case: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseDTO> getCaseDetails(@PathVariable String id) {
        try {
            return ResponseEntity.ok(caseService.getCaseDetails(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/stages")
    public ResponseEntity<List<StageDTO>> getCaseStages(@PathVariable String id) {
        return ResponseEntity.ok(caseService.getStages(id));
    }

    @PostMapping("/{caseId}/tasks/{taskId}/complete")
    public ResponseEntity<String> completeTask(@PathVariable String caseId, @PathVariable String taskId,
            @RequestBody Map<String, Object> variables,
            @RequestParam(required = false, defaultValue = "user") String userId) {
        try {
            caseService.completeTask(taskId, variables, userId);
            return ResponseEntity.ok("Task completed");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error completing task: " + e.getMessage());
        }
    }
}
