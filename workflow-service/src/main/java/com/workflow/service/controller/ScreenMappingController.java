package com.workflow.service.controller;

import com.workflow.service.entity.ScreenMapping;
import com.workflow.service.service.ScreenMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend access
public class ScreenMappingController {

    private final ScreenMappingService screenMappingService;

    @GetMapping("/{stageCode}/mapping")
    public ResponseEntity<ScreenMapping> getMapping(@PathVariable String stageCode) {
        return screenMappingService.getMappingByStageCode(stageCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{stageCode}/mapping")
    public ResponseEntity<?> updateMapping(@PathVariable String stageCode, @RequestBody ScreenMapping request) {
        try {
            ScreenMapping mapping = screenMappingService.updateMapping(stageCode, request.getScreenCode(),
                    request.getAccessType());
            return ResponseEntity.ok(mapping);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
