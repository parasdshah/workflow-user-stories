package com.workflow.service.controller;

import com.workflow.service.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.repository.Deployment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @GetMapping("/preview/{workflowCode}")
    public ResponseEntity<String> previewBpmn(@PathVariable String workflowCode) {
        // F.6 Preview BPMN XML
        return ResponseEntity.ok(deploymentService.previewBpmn(workflowCode));
    }

    @PostMapping("/{workflowCode}")
    public ResponseEntity<String> deployWorkflow(@PathVariable String workflowCode) {
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

    @GetMapping
    public ResponseEntity<List<com.workflow.service.dto.DeploymentHistoryDTO>> getHistory(
            @RequestParam(required = false) String workflowCode) {
        // J.8 View deployment history
        return ResponseEntity.ok(deploymentService.getDeploymentHistory(workflowCode));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> undeploy(@PathVariable String id) {
        try {
            deploymentService.undeployWorkflow(id);
            return ResponseEntity.ok("Deployment deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Undeploy failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/rollback")
    public ResponseEntity<String> rollback(@PathVariable String id) {
        try {
            Deployment deployment = deploymentService.rollbackWorkflow(id);
            return ResponseEntity.ok("Rolled back to deployment: " + deployment.getId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Rollback failed: " + e.getMessage());
        }
    }

}
