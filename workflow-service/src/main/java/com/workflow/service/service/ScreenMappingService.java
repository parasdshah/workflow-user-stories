package com.workflow.service.service;

import com.workflow.service.entity.ScreenMapping;
import com.workflow.service.repository.ScreenMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScreenMappingService {

    private final ScreenMappingRepository screenMappingRepository;
    private final ScreenDefinitionService screenDefinitionService;

    public Optional<ScreenMapping> getMappingByStageCode(String stageCode) {
        return screenMappingRepository.findByStageCode(stageCode).stream().findFirst();
    }

    @Transactional
    public ScreenMapping updateMapping(String stageCode, String screenCode, ScreenMapping.AccessType accessType) {
        if (!screenDefinitionService.exists(screenCode)) {
            throw new IllegalArgumentException("Screen Code " + screenCode + " does not exist.");
        }

        ScreenMapping mapping = screenMappingRepository.findByStageCode(stageCode).stream()
                .findFirst()
                .orElse(new ScreenMapping());

        mapping.setStageCode(stageCode);
        mapping.setScreenCode(screenCode); // G.2 implemented here as well
        mapping.setAccessType(accessType);

        return screenMappingRepository.save(mapping);
    }
}
