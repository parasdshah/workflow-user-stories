package com.workflow.service.controller;

import com.workflow.service.service.SystemResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Slf4j
public class SystemController {

    private final SystemResetService systemResetService;

    @org.springframework.web.bind.annotation.RequestMapping(value = "/reset", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public ResponseEntity<String> resetSystem() {
        log.info("Received request to RESET system.");
        try {
            systemResetService.resetSystem();
            return ResponseEntity.ok("System reset successful. All workflows undeployed and configuration data cleaned.");
        } catch (Exception e) {
            log.error("System reset failed", e);
            return ResponseEntity.internalServerError().body("System reset failed: " + e.getMessage());
        }
    }
}
