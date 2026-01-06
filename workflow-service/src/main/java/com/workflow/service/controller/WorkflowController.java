package com.workflow.service.controller;

import com.workflow.service.entity.ScreenMapping;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.ScreenDefinition;

import com.workflow.service.entity.WorkflowMaster;
import com.workflow.service.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Simplest for now to avoid CORS/Proxy issues
@Tag(name = "Workflow Management", description = "APIs for managing workflow definitions, stages, screens, and configurations")
public class WorkflowController {

    private final WorkflowDefinitionService workflowService;

    // Workflow Endpoints
    @Operation(summary = "Create or update a workflow", description = "Creates a new workflow definition or updates an existing one")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workflow created/updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid workflow data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<?> createWorkflow(
            @Parameter(description = "Workflow definition") @RequestBody WorkflowMaster workflow,
            @Parameter(description = "User ID performing the operation") @RequestHeader(value = "X-User-Id", defaultValue = "system") String user) {
        try {
            return ResponseEntity.ok(workflowService.saveWorkflow(workflow, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving workflow: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all workflows", description = "Retrieves all workflow definitions")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved workflows")
    @GetMapping
    public ResponseEntity<List<WorkflowMaster>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    @Operation(summary = "Get workflow by code", description = "Retrieves a specific workflow definition by its code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workflow found"),
            @ApiResponse(responseCode = "404", description = "Workflow not found")
    })
    @GetMapping("/{code}")
    public ResponseEntity<WorkflowMaster> getWorkflow(
            @Parameter(description = "Workflow code") @PathVariable String code) {
        return workflowService.getWorkflow(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get workflow statistics", description = "Retrieves statistics for all workflows including case counts")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics")
    @GetMapping("/stats")
    public ResponseEntity<List<com.workflow.service.dto.WorkflowStatsDTO>> getWorkflowStats() {
        return ResponseEntity.ok(workflowService.getWorkflowStats());
    }

    @Operation(summary = "Get global process graph", description = "Retrieves the complete nested graph including child workflows")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved global graph")
    @GetMapping("/{code}/global-graph")
    public ResponseEntity<com.workflow.service.dto.GraphDTO> getGlobalGraph(
            @Parameter(description = "Root Workflow code") @PathVariable String code) {
        return ResponseEntity.ok(workflowService.getGlobalGraph(code));
    }

    // Stage Endpoints
    @Operation(summary = "Add stage to workflow", description = "Adds a new stage configuration to an existing workflow")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stage added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid stage data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{code}/stages")
    public ResponseEntity<?> addStage(
            @Parameter(description = "Workflow code") @PathVariable String code,
            @Parameter(description = "Stage configuration") @RequestBody StageConfig stage,
            @Parameter(description = "User ID performing the operation") @RequestHeader(value = "X-User-Id", defaultValue = "system") String user) {
        stage.setWorkflowCode(code);
        try {
            return ResponseEntity.ok(workflowService.saveStage(stage, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving stage: " + e.getMessage());
        }
    }

    @Operation(summary = "Get workflow stages", description = "Retrieves all stages for a specific workflow")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved stages")
    @GetMapping("/{code}/stages")
    public ResponseEntity<List<StageConfig>> getStages(
            @Parameter(description = "Workflow code") @PathVariable String code) {
        return ResponseEntity.ok(workflowService.getStages(code));
    }

    @Operation(summary = "Delete stage", description = "Deletes a stage from a workflow")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stage deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{code}/stages/{stageCode}")
    public ResponseEntity<?> deleteStage(
            @Parameter(description = "Workflow code") @PathVariable String code,
            @Parameter(description = "Stage code") @PathVariable String stageCode) {
        try {
            workflowService.deleteStage(code, stageCode);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting stage: " + e.getMessage());
        }
    }

    // Screen Mapping Endpoints
    @Operation(summary = "Add screen mapping", description = "Maps a screen definition to a workflow stage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Screen mapping added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid mapping data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/stages/{stageCode}/screens")
    public ResponseEntity<?> addScreenMapping(
            @Parameter(description = "Stage code") @PathVariable String stageCode,
            @Parameter(description = "Screen mapping configuration") @RequestBody ScreenMapping mapping,
            @Parameter(description = "User ID performing the operation") @RequestHeader(value = "X-User-Id", defaultValue = "system") String user) {
        mapping.setStageCode(stageCode);
        try {
            return ResponseEntity.ok(workflowService.saveScreenMapping(mapping, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving screen mapping: " + e.getMessage());
        }
    }

    @Operation(summary = "Get screen mappings", description = "Retrieves all screen mappings for a specific stage")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved screen mappings")
    @GetMapping("/stages/{stageCode}/screens")
    public ResponseEntity<List<ScreenMapping>> getScreenMappings(
            @Parameter(description = "Stage code") @PathVariable String stageCode) {
        return ResponseEntity.ok(workflowService.getScreenMappings(stageCode));
    }

    // Screen Definition Endpoints

    @Operation(summary = "Create screen definition", description = "Creates a new screen definition for use in workflows")
    @ApiResponse(responseCode = "200", description = "Screen definition created successfully")
    @PostMapping("/screens")
    public ResponseEntity<ScreenDefinition> createScreenDefinition(
            @Parameter(description = "Screen definition") @RequestBody ScreenDefinition screen,
            @Parameter(description = "User ID performing the operation") @RequestHeader(value = "X-User-Id", defaultValue = "system") String user) {
        return ResponseEntity.ok(workflowService.saveScreenDefinition(screen, user));
    }

    @Operation(summary = "Get all screen definitions", description = "Retrieves all available screen definitions")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved screen definitions")
    @GetMapping("/screens")
    public ResponseEntity<List<ScreenDefinition>> getAllScreenDefinitions() {
        return ResponseEntity.ok(workflowService.getAllScreenDefinitions());
    }

    @Operation(summary = "Get screen definition by code", description = "Retrieves a specific screen definition by its code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Screen definition found"),
            @ApiResponse(responseCode = "404", description = "Screen definition not found")
    })
    @GetMapping("/screens/{code}")
    public ResponseEntity<ScreenDefinition> getScreenDefinition(
            @Parameter(description = "Screen code") @PathVariable String code) {
        return workflowService.getScreenDefinition(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
