package com.workflow.service.controller;

import com.workflow.service.service.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.repository.Deployment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
@Tag(name = "BPMN Deployment", description = "APIs for deploying, previewing, and managing BPMN workflow deployments")
public class DeploymentController {

    private final DeploymentService deploymentService;

    @Operation(summary = "Preview BPMN XML", description = "Generates and previews the BPMN XML for a workflow without deploying it")
    @ApiResponse(responseCode = "200", description = "Successfully generated BPMN XML")
    @GetMapping("/preview/{workflowCode}")
    public ResponseEntity<String> previewBpmn(
            @Parameter(description = "Workflow code") @PathVariable String workflowCode) {
        // F.6 Preview BPMN XML
        return ResponseEntity.ok(deploymentService.previewBpmn(workflowCode));
    }

    @Operation(summary = "Deploy workflow", description = "Deploys a workflow definition to the Flowable engine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workflow deployed successfully"),
            @ApiResponse(responseCode = "500", description = "Deployment failed")
    })
    @PostMapping("/{workflowCode}")
    public ResponseEntity<String> deployWorkflow(
            @Parameter(description = "Workflow code") @PathVariable String workflowCode) {
        // F.7 / J.1 One-click BPMN deployment
        try {
            Deployment deployment = deploymentService.deployWorkflow(workflowCode);
            return ResponseEntity.ok(deployment.getId());
        } catch (Exception e) {
            // e.printStackTrace();
            // log property is available due to @Slf4j on class? No, let's check.
            // DeploymentController was modified to add logging?
            // Wait, I need to check if @Slf4j is present.
            org.slf4j.LoggerFactory.getLogger(DeploymentController.class).error("Deployment failed", e);
            return ResponseEntity.internalServerError().body("Deployment failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Get deployment history", description = "Retrieves deployment history, optionally filtered by workflow code")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved deployment history")
    @GetMapping
    public ResponseEntity<List<com.workflow.service.dto.DeploymentHistoryDTO>> getHistory(
            @Parameter(description = "Optional workflow code filter") @RequestParam(required = false) String workflowCode) {
        // J.8 View deployment history
        return ResponseEntity.ok(deploymentService.getDeploymentHistory(workflowCode));
    }

    @Operation(summary = "Undeploy workflow", description = "Removes a deployed workflow from the Flowable engine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deployment deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Undeploy failed")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> undeploy(
            @Parameter(description = "Deployment ID") @PathVariable String id) {
        try {
            deploymentService.undeployWorkflow(id);
            return ResponseEntity.ok("Deployment deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Undeploy failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Rollback workflow", description = "Rolls back to a previous deployment version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rollback successful"),
            @ApiResponse(responseCode = "500", description = "Rollback failed")
    })
    @PostMapping("/{id}/rollback")
    public ResponseEntity<String> rollback(
            @Parameter(description = "Deployment ID to rollback to") @PathVariable String id) {
        try {
            Deployment deployment = deploymentService.rollbackWorkflow(id);
            return ResponseEntity.ok("Rolled back to deployment: " + deployment.getId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Rollback failed: " + e.getMessage());
        }
    }

}
