package com.workflow.service.controller;

import com.workflow.service.service.RuntimeStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/runtime")
@RequiredArgsConstructor
@Tag(name = "Runtime Status", description = "APIs for querying runtime status of workflow cases")
public class RuntimeController {

    private final RuntimeStatusService runtimeStatusService;

    @Operation(summary = "Get case status", description = "Retrieves the runtime status of a workflow case (ACTIVE or COMPLETED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Case status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    @GetMapping("/cases/{caseId}/status")
    public ResponseEntity<String> getCaseStatus(
            @Parameter(description = "Case ID") @PathVariable String caseId) {
        ProcessInstance instance = runtimeStatusService.getCaseStatus(caseId);
        if (instance == null) {
            // Check history or return not found
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(instance.isEnded() ? "COMPLETED" : "ACTIVE");
    }

}
