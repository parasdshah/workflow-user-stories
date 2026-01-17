package com.workflow.service.controller;

import com.workflow.service.service.CaseService;
import com.workflow.service.dto.UserWorkloadDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/runtime/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Workflow Stats", description = "APIs for workflow statistics and workload analysis")
public class StatsController {

    private final CaseService caseService;

    @Operation(summary = "Get user workload", description = "Retrieves aggregated count of pending cases for each user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved workload stats")
    @GetMapping("/workload")
    public ResponseEntity<List<UserWorkloadDTO>> getUserWorkload() {
        return ResponseEntity.ok(caseService.getUserWorkload());
    }
}
