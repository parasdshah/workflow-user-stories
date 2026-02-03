package com.workflow.service.controller;

import com.workflow.service.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/runtime/cases")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Case Reassignment", description = "Endpoints for administrative task reassignment")
public class CaseReassignmentController {

    private final CaseService caseService;

    @Operation(summary = "Reassign a task (Admin)", description = "Reassigns a task to a new user with an audit reason")
    @ApiResponse(responseCode = "200", description = "Task reassigned successfully")
    @PostMapping("/{caseId}/tasks/{taskId}/reassign")
    public ResponseEntity<String> reassignTask(
            @Parameter(description = "Case ID") @PathVariable String caseId,
            @Parameter(description = "Task ID") @PathVariable String taskId,
            @RequestBody ReassignRequest request,
            @RequestParam String adminUserId) {
        
        try {
            log.info("Request to reassign task {} in case {} to user {} by admin {}", 
                taskId, caseId, request.getNewAssignee(), adminUserId);
                
            caseService.reassignTask(taskId, request.getNewAssignee(), request.getReason(), adminUserId);
            return ResponseEntity.ok("Task reassigned successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error reassigning task", e);
            return ResponseEntity.internalServerError().body("Error reassigning task: " + e.getMessage());
        }
    }

    @Data
    public static class ReassignRequest {
        private String newAssignee;
        private String reason;
    }
}
