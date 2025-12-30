package com.workflow.service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.service.entity.StageAction;
import com.workflow.service.entity.StageConfig;
import com.workflow.service.repository.StageConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMigrationService {

    private final StageConfigRepository stageConfigRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    @Transactional
    public void migrateLegacyActions() {
        log.info("Checking for legacy actions to migrate...");
        List<StageConfig> stages = stageConfigRepository.findAll();
        int count = 0;

        for (StageConfig stage : stages) {
            String legacy = stage.getAllowedActionsLegacy();
            if (legacy != null && !legacy.isBlank()) {
                log.info("Migrating actions for stage: {}", stage.getStageCode());
                try {
                    List<StageAction> newActions = new ArrayList<>();
                    
                    if (legacy.trim().startsWith("[")) {
                        // JSON Array
                        try {
                            List<Map<String, String>> jsonActions = objectMapper.readValue(legacy, new TypeReference<List<Map<String, String>>>(){});
                            for (Map<String, String> json : jsonActions) {
                                StageAction action = new StageAction();
                                action.setStageConfig(stage);
                                action.setActionLabel(json.get("label") != null ? json.get("label") : json.get("value"));
                                action.setButtonStyle(json.get("style"));
                                action.setTargetType(json.get("target"));
                                action.setTargetStage(json.get("targetStage"));
                                action.setPostActionStatus(json.get("status"));
                                
                                // Defaults
                                if (action.getButtonStyle() == null) action.setButtonStyle("primary");
                                if (action.getTargetType() == null) action.setTargetType("NEXT");
                                
                                newActions.add(action);
                            }
                        } catch (Exception e) {
                             // Fallback for simple string array helper (if it was ["A", "B"])
                             // But usually our JSON was more complex.
                             log.warn("Complex JSON parse failed, trying simple array: {}", e.getMessage());
                        }
                    } else {
                        // Comma Separated String (Oldest Legacy)
                        String[] parts = legacy.split(",");
                        for (String part : parts) {
                            if (part.isBlank()) continue;
                            StageAction action = new StageAction();
                            action.setStageConfig(stage);
                            action.setActionLabel(part.trim());
                            action.setButtonStyle("primary");
                            action.setTargetType("NEXT");
                            newActions.add(action);
                        }
                    }

                    if (!newActions.isEmpty()) {
                        stage.getActions().clear();
                        stage.getActions().addAll(newActions);
                        stage.setAllowedActionsLegacy(null); // Mark migrated
                        stageConfigRepository.save(stage);
                        count++;
                    }

                } catch (Exception e) {
                    log.error("Failed to migrate stage {}: {}", stage.getStageCode(), e.getMessage());
                }
            }
        }
        
        if (count > 0) {
            log.info("Successfully migrated actions for {} stages.", count);
        } else {
            log.info("No legacy actions found to migrate.");
        }
    }
}
