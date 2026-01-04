package com.workflow.service.controller;

import com.workflow.service.dto.CaseDTO;
import com.workflow.service.dto.InitiateCaseRequest;
import com.workflow.service.dto.StageDTO;
import com.workflow.service.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/runtime/cases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Case Runtime", description = "APIs for managing workflow case instances and task execution")
public class CaseController {

    private final CaseService caseService;

    @Operation(summary = "Get all active cases", description = "Retrieves all currently active workflow case instances")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved active cases")
    @GetMapping
    public ResponseEntity<List<CaseDTO>> getAllActiveCases() {
        return ResponseEntity.ok(caseService.getAllActiveCases());
    }

    @Operation(summary = "Initiate a new case", description = "Starts a new workflow case instance with the specified workflow code and variables")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Case initiated successfully, returns case ID"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<String> initiateCase(
            @Parameter(description = "Case initiation request with workflow code and variables") @RequestBody InitiateCaseRequest request) {
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

    @Operation(summary = "Get case details", description = "Retrieves detailed information about a specific case instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Case details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CaseDTO> getCaseDetails(
            @Parameter(description = "Case ID") @PathVariable String id) {
        try {
            return ResponseEntity.ok(caseService.getCaseDetails(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get case stages", description = "Retrieves all stages/tasks for a specific case")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved case stages")
    @GetMapping("/{id}/stages")
    public ResponseEntity<List<StageDTO>> getCaseStages(
            @Parameter(description = "Case ID") @PathVariable String id) {
        return ResponseEntity.ok(caseService.getStages(id));
    }

    @Operation(summary = "Get user task history", description = "Retrieves task history for a specific user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved task history")
    @GetMapping("/tasks/history")
    public ResponseEntity<List<StageDTO>> getUserTaskHistory(
            @Parameter(description = "User ID") @RequestParam String userId) {
        return ResponseEntity.ok(caseService.getUserTaskHistory(userId));
    }

    @Operation(summary = "Complete a task", description = "Completes a specific task within a case with the provided variables")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{caseId}/tasks/{taskId}/complete")
    public ResponseEntity<String> completeTask(
            @Parameter(description = "Case ID") @PathVariable String caseId,
            @Parameter(description = "Task ID") @PathVariable String taskId,
            @Parameter(description = "Task completion variables") @RequestBody Map<String, Object> variables,
            @Parameter(description = "User ID performing the task") @RequestParam(required = false, defaultValue = "user") String userId) {
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
