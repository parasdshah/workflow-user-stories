package com.workflow.service.controller;

import com.workflow.service.entity.ScreenMapping;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.ScreenDefinition;

import com.workflow.service.entity.WorkflowMaster;
import com.workflow.service.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Simplest for now to avoid CORS/Proxy issues
public class WorkflowController {

    private final WorkflowDefinitionService workflowService;

    // Workflow Endpoints
    @PostMapping
    public ResponseEntity<?> createWorkflow(@RequestBody WorkflowMaster workflow,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String user) {
        try {
            return ResponseEntity.ok(workflowService.saveWorkflow(workflow, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving workflow: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<WorkflowMaster>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    @GetMapping("/{code}")
    public ResponseEntity<WorkflowMaster> getWorkflow(@PathVariable String code) {
        return workflowService.getWorkflow(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<List<com.workflow.service.dto.WorkflowStatsDTO>> getWorkflowStats() {
        return ResponseEntity.ok(workflowService.getWorkflowStats());
    }

    // Stage Endpoints
    @PostMapping("/{code}/stages")
    public ResponseEntity<?> addStage(@PathVariable String code,
            @RequestBody StageConfig stage,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String user) {
        stage.setWorkflowCode(code);
        try {
            return ResponseEntity.ok(workflowService.saveStage(stage, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving stage: " + e.getMessage());
        }
    }

    @GetMapping("/{code}/stages")
    public ResponseEntity<List<StageConfig>> getStages(@PathVariable String code) {
        return ResponseEntity.ok(workflowService.getStages(code));
    }

    @DeleteMapping("/{code}/stages/{stageCode}")
    public ResponseEntity<?> deleteStage(@PathVariable String code, @PathVariable String stageCode) {
        try {
            workflowService.deleteStage(code, stageCode);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting stage: " + e.getMessage());
        }
    }

    // Screen Mapping Endpoints
    @PostMapping("/stages/{stageCode}/screens")
    public ResponseEntity<?> addScreenMapping(@PathVariable String stageCode,
            @RequestBody ScreenMapping mapping,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String user) {
        mapping.setStageCode(stageCode);
        try {
            return ResponseEntity.ok(workflowService.saveScreenMapping(mapping, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving screen mapping: " + e.getMessage());
        }
    }

    @GetMapping("/stages/{stageCode}/screens")
    public ResponseEntity<List<ScreenMapping>> getScreenMappings(@PathVariable String stageCode) {
        return ResponseEntity.ok(workflowService.getScreenMappings(stageCode));
    }

    // Screen Definition Endpoints

    @PostMapping("/screens")
    public ResponseEntity<ScreenDefinition> createScreenDefinition(@RequestBody ScreenDefinition screen,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String user) {
        return ResponseEntity.ok(workflowService.saveScreenDefinition(screen, user));
    }

    @GetMapping("/screens")
    public ResponseEntity<List<ScreenDefinition>> getAllScreenDefinitions() {
        return ResponseEntity.ok(workflowService.getAllScreenDefinitions());
    }

    @GetMapping("/screens/{code}")
    public ResponseEntity<ScreenDefinition> getScreenDefinition(@PathVariable String code) {
        return workflowService.getScreenDefinition(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
