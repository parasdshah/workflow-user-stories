package com.workflow.service.controller;

import com.workflow.service.service.SystemResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Management", description = "APIs for system-level operations and maintenance")
public class SystemController {

    private final SystemResetService systemResetService;

    @Operation(summary = "Reset system", description = "⚠️ WARNING: Destructive operation! Undeploys all workflows and cleans all configuration data. Use only in development/testing environments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System reset successful"),
            @ApiResponse(responseCode = "500", description = "System reset failed")
    })
    @org.springframework.web.bind.annotation.RequestMapping(value = "/reset", method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST })
    public ResponseEntity<String> resetSystem() {
        log.info("Received request to RESET system.");
        try {
            systemResetService.resetSystem();
            return ResponseEntity
                    .ok("System reset successful. All workflows undeployed and configuration data cleaned.");
        } catch (Exception e) {
            log.error("System reset failed", e);
            return ResponseEntity.internalServerError().body("System reset failed: " + e.getMessage());
        }
    }
}
