package com.workflow.service.controller;

import com.workflow.service.entity.AuditTrail;
import com.workflow.service.repository.AuditTrailRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "APIs for querying audit trail and change history")
public class AuditLogController {

    @Autowired
    private AuditTrailRepository auditTrailRepository;

    @Operation(summary = "Get audit logs", description = "Retrieves audit logs with optional filtering by entity, action, user, and date range. Supports pagination and sorting.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs")
    @GetMapping
    public Page<AuditTrail> getAuditLogs(
            @Parameter(description = "Filter by entity name") @RequestParam(required = false) String entityName,
            @Parameter(description = "Filter by action (CREATE, UPDATE, DELETE)") @RequestParam(required = false) String action,
            @Parameter(description = "Filter by user who made the change") @RequestParam(required = false) String changedBy,
            @Parameter(description = "Filter by start date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter by end date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "changedAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<AuditTrail> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (entityName != null && !entityName.isEmpty()) {
                predicates.add(cb.equal(root.get("entityName"), entityName));
            }
            if (action != null && !action.isEmpty()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (changedBy != null && !changedBy.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("changedBy")), "%" + changedBy.toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("changedAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("changedAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditTrailRepository.findAll(spec, pageable);
    }
}
