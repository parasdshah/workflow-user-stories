package com.workflow.service.util;

import com.workflow.service.entity.StageConfig;
import com.workflow.service.repository.StageConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixDuplicates implements CommandLineRunner {

    private final StageConfigRepository stageRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking for duplicate stages...");
        // Specifically fix Rating s2
        List<StageConfig> stages = stageRepository.findByWorkflowCodeOrderBySequenceOrderAsc("Rating");
        List<StageConfig> s2Stages = stages.stream()
                .filter(s -> "s2".equals(s.getStageCode()))
                .collect(Collectors.toList());

        if (s2Stages.size() > 1) {
            log.info("Found {} duplicates for Rating s2. Fixing...", s2Stages.size());
            
            StageConfig target = null;
            // Prefer the one with Actions (ID 34)
            for (StageConfig s : s2Stages) {
                if (s.getActions() != null && !s.getActions().isEmpty()) {
                    target = s;
                }
            }
            
            // If no actions, just pick first
            if (target == null) target = s2Stages.get(0);
            
            // Update target with Assignment Rules if missing
            if (target.getAssignmentRules() == null || target.getAssignmentRules().isBlank()) {
                target.setAssignmentRules("{\"mechanism\":\"ROUND_ROBIN\",\"roundRobinPool\":\"managers\"}");
                stageRepository.save(target);
                log.info("Updated target stage {} with assignment rules", target.getId());
            }

            // Delete others
            for (StageConfig s : s2Stages) {
                if (!s.getId().equals(target.getId())) {
                    stageRepository.delete(s);
                    log.info("Deleted duplicate stage {}", s.getId());
                }
            }
        } else {
            log.info("No duplicates found for Rating s2.");
        }
    }
}
