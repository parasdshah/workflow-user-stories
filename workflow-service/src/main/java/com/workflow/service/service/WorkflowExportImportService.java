package com.workflow.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.service.dto.WorkflowExportDto;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.entity.WorkflowMaster;
import com.workflow.service.repository.StageConfigRepository;
import com.workflow.service.repository.WorkflowMasterRepository;
import com.workflow.service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowExportImportService {

    private final WorkflowMasterRepository workflowRepository;
    private final StageConfigRepository stageRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public byte[] exportWorkflow(String workflowCode, boolean encrypted) {
        try {
            log.info("Exporting workflow: {} (Encrypted: {})", workflowCode, encrypted);
            Optional<WorkflowMaster> workflowOpt = workflowRepository.findByWorkflowCode(workflowCode);
            if (workflowOpt.isEmpty()) {
                throw new RuntimeException("Workflow not found: " + workflowCode);
            }

            List<StageConfig> stages = stageRepository.findByWorkflowCodeOrderBySequenceOrderAsc(workflowCode);

            WorkflowExportDto exportDto = WorkflowExportDto.builder()
                    .workflow(workflowOpt.get())
                    .stages(stages)
                    .build();

            String json = objectMapper.writeValueAsString(exportDto);
            
            if (encrypted) {
                String encryptedContent = securityUtils.encrypt(json);
                return encryptedContent.getBytes(StandardCharsets.UTF_8);
            } else {
                return json.getBytes(StandardCharsets.UTF_8);
            }

        } catch (Exception e) {
            log.error("Error exporting workflow", e);
            throw new RuntimeException("Export failed", e);
        }
    }

    @Transactional
    public void importWorkflow(MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            log.info("Importing workflow from file: {}", filename);
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String json;

            // Simple Heuristic: If it ends in .json OR starts with '{', assume JSON. 
            // Otherwise, assume Encrypted.
            boolean isJson = (filename != null && filename.endsWith(".json")) || content.trim().startsWith("{");

            if (isJson) {
                json = content;
            } else {
                try {
                    json = securityUtils.decrypt(content);
                } catch (Exception e) {
                    // Fallback: If decryption fails, maybe it IS json but named oddly?
                    if (content.trim().startsWith("{")) {
                        json = content;
                    } else {
                        throw new RuntimeException("Decryption failed and content does not appear to be JSON");
                    }
                }
            }

            WorkflowExportDto dto = objectMapper.readValue(json, WorkflowExportDto.class);
            WorkflowMaster importedWf = dto.getWorkflow();
            List<StageConfig> importedStages = dto.getStages();

            if (importedWf == null || importedWf.getWorkflowCode() == null) {
                throw new RuntimeException("Invalid workflow master data in import file");
            }

            String code = importedWf.getWorkflowCode();
            log.info("Importing workflow definition for code: {}", code);

            // 1. Upsert Workflow Master
            Optional<WorkflowMaster> existingOpt = workflowRepository.findByWorkflowCode(code);
            WorkflowMaster masterToSave;
            if (existingOpt.isPresent()) {
                masterToSave = existingOpt.get();
                // Update fields
                masterToSave.setWorkflowName(importedWf.getWorkflowName());
                masterToSave.setSlaDurationDays(importedWf.getSlaDurationDays());
                masterToSave.setAssociatedModule(importedWf.getAssociatedModule());
                // Preserve ID and Status? Assuming Import implies active unless specified
            } else {
                masterToSave = importedWf;
                masterToSave.setId(null); // Ensure new insert
            }
            workflowRepository.save(masterToSave);

            // 2. Replace Stages
            // Delete existing stages to ensure we exactly match the imported config (handles deleted stages)
            stageRepository.deleteByWorkflowCode(code);

            // Insert imported stages
            if (importedStages != null) {
                for (StageConfig stage : importedStages) {
                    stage.setId(null); // Force new insert
                    stage.setWorkflowCode(code); // Ensure linkage
                    
                    if (stage.getActions() != null) {
                        for (com.workflow.service.entity.StageAction action : stage.getActions()) {
                            action.setId(null); // Force new insert
                            action.setStageConfig(stage); // Ensure relationship
                        }
                    }
                    
                    stageRepository.save(stage);
                }
            }
            
            log.info("Import successful for {}", code);

        } catch (Exception e) {
            log.error("Error importing workflow", e);
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }
}
