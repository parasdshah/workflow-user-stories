package com.workflow.service.controller;

import com.workflow.service.dto.DecisionTableDTO;
import com.workflow.service.service.DmnConversionService;
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
public class RuleController {

    private final DmnRepositoryService dmnRepositoryService;
    private final DmnConversionService dmnConversionService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadRule(
            @RequestParam("file") MultipartFile file,
            @RequestParam("key") String key,
            @RequestParam("name") String name) {

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

    @DeleteMapping("/{deploymentId}")
    public ResponseEntity<Void> deleteRule(@PathVariable String deploymentId) {
        log.info("Deleting Rule Deployment: {}", deploymentId);
        dmnRepositoryService.deleteDeployment(deploymentId);
        return ResponseEntity.noContent().build();
    }
}
