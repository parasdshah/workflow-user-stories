package com.workflow.service.controller;

import com.workflow.service.service.WorkflowExportImportService;
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
public class WorkflowExportImportController {

    private final WorkflowExportImportService exportImportService;

    @GetMapping("/export/{workflowCode}")
    public ResponseEntity<byte[]> exportWorkflow(@PathVariable String workflowCode, 
                                                 @RequestParam(defaultValue = "encrypted") String format) {
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

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importWorkflow(@RequestParam("file") MultipartFile file) {
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
