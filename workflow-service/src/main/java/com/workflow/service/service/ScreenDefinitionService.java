package com.workflow.service.service;

import com.workflow.service.entity.ScreenDefinition;
import com.workflow.service.repository.ScreenDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScreenDefinitionService {

    private final ScreenDefinitionRepository screenDefinitionRepository;

    public List<ScreenDefinition> getAllScreenDefinitions() {
        return screenDefinitionRepository.findAll();
    }

    public Optional<ScreenDefinition> getScreenDefinition(String screenCode) {
        return screenDefinitionRepository.findById(screenCode);
    }

    public ScreenDefinition createOrUpdateScreenDefinition(ScreenDefinition screenDefinition) {
        return screenDefinitionRepository.save(screenDefinition);
    }

    public boolean exists(String screenCode) {
        return screenDefinitionRepository.existsById(screenCode);
    }
}
