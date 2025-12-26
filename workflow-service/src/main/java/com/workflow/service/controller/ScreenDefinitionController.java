package com.workflow.service.controller;

import com.workflow.service.entity.ScreenDefinition;
import com.workflow.service.service.ScreenDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screens")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend access
public class ScreenDefinitionController {

    private final ScreenDefinitionService screenDefinitionService;

    @GetMapping
    public List<ScreenDefinition> getAllScreens() {
        return screenDefinitionService.getAllScreenDefinitions();
    }

    @GetMapping("/{screenCode}")
    public ResponseEntity<ScreenDefinition> getScreen(@PathVariable String screenCode) {
        return screenDefinitionService.getScreenDefinition(screenCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ScreenDefinition createOrUpdateScreen(@RequestBody ScreenDefinition screenDefinition) {
        return screenDefinitionService.createOrUpdateScreenDefinition(screenDefinition);
    }
}
