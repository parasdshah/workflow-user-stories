package com.workflow.service.controller;

import com.workflow.service.entity.ScreenMapping;
import com.workflow.service.service.ScreenMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend access
@Tag(name = "Screen Mappings", description = "APIs for mapping screens to workflow stages")
public class ScreenMappingController {

    private final ScreenMappingService screenMappingService;

    @Operation(summary = "Get screen mapping", description = "Retrieves the screen mapping for a specific stage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mapping found"),
            @ApiResponse(responseCode = "204", description = "No mapping exists for this stage")
    })
    @GetMapping("/{stageCode}/mapping")
    public ResponseEntity<ScreenMapping> getMapping(
            @Parameter(description = "Stage code") @PathVariable String stageCode) {
        return screenMappingService.getMappingByStageCode(stageCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Update screen mapping", description = "Updates or creates a screen mapping for a stage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mapping updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid mapping data")
    })
    @PostMapping("/{stageCode}/mapping")
    public ResponseEntity<?> updateMapping(
            @Parameter(description = "Stage code") @PathVariable String stageCode,
            @Parameter(description = "Screen mapping configuration") @RequestBody ScreenMapping request) {
        try {
            ScreenMapping mapping = screenMappingService.updateMapping(stageCode, request.getScreenCode(),
                    request.getAccessType());
            return ResponseEntity.ok(mapping);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
