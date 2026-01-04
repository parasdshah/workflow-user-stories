package com.workflow.service.controller;

import com.workflow.service.dto.DecisionTableDTO;
import com.workflow.service.service.DmnConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnRepositoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DMN Rules", description = "APIs for managing DMN decision tables and business rules")
public class RuleController {

    private final DmnRepositoryService dmnRepositoryService;
    private final DmnConversionService dmnConversionService;

    @Operation(summary = "Upload DMN rule", description = "Uploads a CSV file and converts it to DMN decision table format")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule uploaded and deployed successfully"),
            @ApiResponse(responseCode = "400", description = "Error reading CSV file"),
            @ApiResponse(responseCode = "500", description = "Error processing rule")
    })
    @PostMapping("/upload")
    public ResponseEntity<String> uploadRule(
            @Parameter(description = "CSV file containing decision table") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Decision table key") @RequestParam("key") String key,
            @Parameter(description = "Decision table name") @RequestParam("name") String name) {

        log.info("Uploading DMN Rule: Key={}, Name={}", key, name);

        try {
            String xml = dmnConversionService.convertCsvToDmnXml(key, name, file.getInputStream());
            log.info("Generated DMN XML for key {}:\n{}", key, xml);

            dmnRepositoryService.createDeployment()
                    .name(name)
                    .category("DYNAMIC_RULE")
                    .addString(key + ".dmn", xml)
                    .deploy();

            return ResponseEntity.ok("Rule uploaded and deployed successfully");

        } catch (IOException e) {
            log.error("Error reading CSV file", e);
            return ResponseEntity.badRequest().body("Error file content: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error converting/deploying rule", e);
            return ResponseEntity.internalServerError().body("Error processing rule: " + e.getMessage());
        }
    }

    @Operation(summary = "List all rules", description = "Retrieves all deployed DMN decision tables (latest versions)")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved decision tables")
    @GetMapping
    public ResponseEntity<List<DecisionTableDTO>> listRules() {
        List<DmnDecision> decisions = dmnRepositoryService.createDecisionQuery()
                .latestVersion()
                .orderByDecisionName().asc()
                .list();

        List<DecisionTableDTO> dtos = decisions.stream().map(dt -> {
            DecisionTableDTO dto = new DecisionTableDTO();
            dto.setId(dt.getId());
            dto.setKey(dt.getKey());
            dto.setName(dt.getName());
            dto.setVersion(dt.getVersion());
            dto.setDeploymentId(dt.getDeploymentId());
            dto.setResourceName(dt.getResourceName());
            dto.setCategory(dt.getCategory());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Delete rule", description = "Deletes a DMN decision table deployment")
    @ApiResponse(responseCode = "204", description = "Rule deleted successfully")
    @DeleteMapping("/{deploymentId}")
    public ResponseEntity<Void> deleteRule(
            @Parameter(description = "Deployment ID") @PathVariable String deploymentId) {
        log.info("Deleting Rule Deployment: {}", deploymentId);
        dmnRepositoryService.deleteDeployment(deploymentId);
        return ResponseEntity.noContent().build();
    }
}
