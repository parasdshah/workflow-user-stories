package com.workflow.hrms.controller;

import com.workflow.hrms.entity.*;
import com.workflow.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hrms")
@RequiredArgsConstructor
public class HrmsManagementController {

    private final RoleMasterRepository roleRepo;
    private final RefRegionRepository regionRepo;
    private final RefProductRepository productRepo;
    private final RefBusinessSegmentRepository segmentRepo; // Injected
    private final RefBusinessSubSegmentRepository subSegmentRepo; // Injected
    private final EmployeeMatrixAssignmentRepository matrixRepo;
    private final EmployeeMasterRepository employeeRepo; // Needed to verify/fetch employee for assignment

    // --- ROLES ---
    @GetMapping("/roles")
    public ResponseEntity<List<RoleMaster>> getAllRoles() {
        return ResponseEntity.ok(roleRepo.findAll());
    }

    @PostMapping("/roles")
    public ResponseEntity<RoleMaster> createRole(@RequestBody RoleMaster role) {
        return ResponseEntity.ok(roleRepo.save(role));
    }

    // --- REGIONS ---
    @GetMapping("/regions")
    public ResponseEntity<List<RefRegion>> getAllRegions() {
        return ResponseEntity.ok(regionRepo.findAll());
    }

    @PostMapping("/regions")
    public ResponseEntity<RefRegion> createRegion(@RequestBody RefRegion region) {
        if (region.getParentRegion() != null && region.getParentRegion().getRegionId() != null) {
            RefRegion parent = regionRepo.findById(region.getParentRegion().getRegionId()).orElseThrow();
            region.setParentRegion(parent);
            RefRegion saved = regionRepo.save(region);
            saved.setPath(parent.getPath() + saved.getRegionId() + "/");
            return ResponseEntity.ok(regionRepo.save(saved));
        } else {
            // Root region logic if needed, simplify for now
            RefRegion saved = regionRepo.save(region);
            saved.setPath("/" + saved.getRegionId() + "/");
            return ResponseEntity.ok(regionRepo.save(saved));
        }
    }

    // --- PRODUCTS ---
    @GetMapping("/products")
    public ResponseEntity<List<RefProduct>> getAllProducts() {
        return ResponseEntity.ok(productRepo.findAll());
    }

    @PostMapping("/products")
    public ResponseEntity<RefProduct> createProduct(@RequestBody RefProduct product) {
        return ResponseEntity.ok(productRepo.save(product));
    }

    // --- SEGMENTS ---
    @GetMapping("/segments")
    public ResponseEntity<List<RefBusinessSegment>> getAllSegments() {
        return ResponseEntity.ok(segmentRepo.findAll());
    }

    @PostMapping("/segments")
    public ResponseEntity<RefBusinessSegment> createSegment(@RequestBody RefBusinessSegment segment) {
        return ResponseEntity.ok(segmentRepo.save(segment));
    }

    // --- SUB-SEGMENTS ---
    @GetMapping("/sub-segments")
    public ResponseEntity<List<RefBusinessSubSegment>> getAllSubSegments() {
        return ResponseEntity.ok(subSegmentRepo.findAll());
    }

    @PostMapping("/sub-segments")
    public ResponseEntity<RefBusinessSubSegment> createSubSegment(@RequestBody RefBusinessSubSegment subSegment) {
        return ResponseEntity.ok(subSegmentRepo.save(subSegment));
    }

    // --- ASSIGNMENTS ---
    @GetMapping("/assignments")
    public ResponseEntity<List<EmployeeMatrixAssignment>> getAllAssignments() {
        return ResponseEntity.ok(matrixRepo.findAll());
    }

    @PostMapping("/assignments")
    public ResponseEntity<EmployeeMatrixAssignment> createAssignment(@RequestBody EmployeeMatrixAssignment assignment) {
        // Hydrate references if needed, or rely on JPA to handle IDs if passed
        // correctly
        return ResponseEntity.ok(matrixRepo.save(assignment));
    }

    // --- EMPLOYEES (Lookup for Assignment) ---
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeMaster>> getAllEmployees() {
        return ResponseEntity.ok(employeeRepo.findAll());
    }

    @GetMapping("/employees/by-role/{roleCode}")
    public ResponseEntity<List<EmployeeMaster>> getEmployeesByRole(@PathVariable String roleCode) {
        List<EmployeeMatrixAssignment> assignments = matrixRepo.findByRoleRoleCode(roleCode);
        List<EmployeeMaster> employees = assignments.stream()
                .map(EmployeeMatrixAssignment::getEmployee)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(employees);
    }
}
