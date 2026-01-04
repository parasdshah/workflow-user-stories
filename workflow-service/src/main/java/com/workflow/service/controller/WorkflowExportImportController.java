package com.workflow.service.controller;

import com.workflow.service.service.WorkflowExportImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Export/Import", description = "APIs for exporting and importing workflow definitions")
public class WorkflowExportImportController {

    private final WorkflowExportImportService exportImportService;

    @Operation(summary = "Export workflow", description = "Exports a workflow definition as either encrypted (.enc) or JSON format")
    @ApiResponse(responseCode = "200", description = "Workflow exported successfully")
    @GetMapping("/export/{workflowCode}")
    public ResponseEntity<byte[]> exportWorkflow(
            @Parameter(description = "Workflow code") @PathVariable String workflowCode,
            @Parameter(description = "Export format: 'json' or 'encrypted' (default)") @RequestParam(defaultValue = "encrypted") String format) {
        log.info("Request to export workflow: {}, format: {}", workflowCode, format);

        boolean isEncrypted = !"json".equalsIgnoreCase(format);
        byte[] fileContent = exportImportService.exportWorkflow(workflowCode, isEncrypted);

        String ext = isEncrypted ? ".enc" : ".json";
        String filename = "workflow_" + workflowCode + "_" + System.currentTimeMillis() + ext;

        MediaType mediaType = isEncrypted ? MediaType.APPLICATION_OCTET_STREAM : MediaType.APPLICATION_JSON;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(fileContent);
    }

    @Operation(summary = "Import workflow", description = "Imports a workflow definition from an uploaded file (supports both encrypted and JSON formats)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workflow imported successfully"),
            @ApiResponse(responseCode = "500", description = "Import failed")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importWorkflow(
            @Parameter(description = "Workflow file (.enc or .json)") @RequestParam("file") MultipartFile file) {
        log.info("Request to import workflow file");
        try {
            exportImportService.importWorkflow(file);
            return ResponseEntity.ok("Workflow imported successfully");
        } catch (Exception e) {
            log.error("Import failed at controller", e);
            return ResponseEntity.status(500).body("Import failed: " + e.getMessage());
        }
    }
}
