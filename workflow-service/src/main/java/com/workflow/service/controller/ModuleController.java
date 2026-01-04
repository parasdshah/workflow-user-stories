package com.workflow.service.controller;

import com.workflow.service.entity.Module;
import com.workflow.service.repository.ModuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@Tag(name = "Module Management", description = "APIs for managing application modules")
public class ModuleController {

    private final ModuleRepository moduleRepository;

    @Operation(summary = "Get all modules", description = "Retrieves all application modules")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved modules")
    @GetMapping
    public List<Module> getAllModules() {
        return moduleRepository.findAll();
    }

    @Operation(summary = "Create module", description = "Creates a new application module")
    @ApiResponse(responseCode = "200", description = "Module created successfully")
    @PostMapping
    public Module createModule(
            @Parameter(description = "Module definition") @RequestBody Module module) {
        return moduleRepository.save(module);
    }

    @Operation(summary = "Delete module", description = "Deletes an application module by ID")
    @ApiResponse(responseCode = "200", description = "Module deleted successfully")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModule(
            @Parameter(description = "Module ID") @PathVariable Long id) {
        moduleRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
