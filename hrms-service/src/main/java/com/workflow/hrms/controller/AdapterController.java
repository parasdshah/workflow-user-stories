package com.workflow.hrms.controller;

import com.workflow.hrms.dto.ResolutionRequest;
import com.workflow.hrms.dto.ResolutionResponse;
import com.workflow.hrms.dto.UserAttributes;
import com.workflow.hrms.entity.EmployeeMaster;
import com.workflow.hrms.entity.EmployeeMatrixAssignment;
import com.workflow.hrms.repository.EmployeeMasterRepository;
import com.workflow.hrms.repository.EmployeeMatrixAssignmentRepository;
import com.workflow.hrms.service.MatrixResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/adapter")
@RequiredArgsConstructor
public class AdapterController {

    private final MatrixResolutionService resolutionService;
    private final EmployeeMasterRepository employeeRepo;
    private final EmployeeMatrixAssignmentRepository matrixRepo;

    @PostMapping("/resolve-users")
    public ResponseEntity<ResolutionResponse> resolveUsers(@RequestBody ResolutionRequest request) {
        return ResponseEntity.ok(resolutionService.resolveUsers(request));
    }

    @GetMapping("/users/{userId}/attributes")
    public ResponseEntity<UserAttributes> getUserAttributes(@PathVariable String userId) {
        return employeeRepo.findById(userId)
            .map(emp -> {
                UserAttributes attr = new UserAttributes();
                attr.setUserId(emp.getEmployeeId());
                attr.setFullName(emp.getFullName());
                attr.setEmail(emp.getEmail());
                
                // Aggregate Limits: Find max limit across all assignments for this user
                List<EmployeeMatrixAssignment> assignments = matrixRepo.findByEmployeeEmployeeId(userId);
                
                BigDecimal maxLimit = assignments.stream()
                        .map(EmployeeMatrixAssignment::getApprovalLimit)
                        .filter(java.util.Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(BigDecimal.ZERO);

                attr.setApprovalLimit(maxLimit);
                attr.setRole("Variable (Matrix)"); // Role depends on context
                
                return ResponseEntity.ok(attr);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
