package com.workflow.service.controller;

import com.workflow.service.entity.ScreenDefinition;
import com.workflow.service.service.ScreenDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screens")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend access
@Tag(name = "Screen Definitions", description = "APIs for managing UI screen definitions")
public class ScreenDefinitionController {

    private final ScreenDefinitionService screenDefinitionService;

    @Operation(summary = "Get all screens", description = "Retrieves all screen definitions")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved screens")
    @GetMapping
    public List<ScreenDefinition> getAllScreens() {
        return screenDefinitionService.getAllScreenDefinitions();
    }

    @Operation(summary = "Get screen by code", description = "Retrieves a specific screen definition by its code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Screen found"),
            @ApiResponse(responseCode = "404", description = "Screen not found")
    })
    @GetMapping("/{screenCode}")
    public ResponseEntity<ScreenDefinition> getScreen(
            @Parameter(description = "Screen code") @PathVariable String screenCode) {
        return screenDefinitionService.getScreenDefinition(screenCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create or update screen", description = "Creates a new screen definition or updates an existing one")
    @ApiResponse(responseCode = "200", description = "Screen created/updated successfully")
    @PostMapping
    public ScreenDefinition createOrUpdateScreen(
            @Parameter(description = "Screen definition") @RequestBody ScreenDefinition screenDefinition) {
        return screenDefinitionService.createOrUpdateScreenDefinition(screenDefinition);
    }
}
