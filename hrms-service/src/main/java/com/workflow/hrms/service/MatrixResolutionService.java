package com.workflow.hrms.service;

import com.workflow.hrms.dto.ResolutionRequest;
import com.workflow.hrms.dto.ResolutionResponse;
import com.workflow.hrms.entity.*;
import com.workflow.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatrixResolutionService {

    private final RefRegionRepository regionRepo;
    private final RefProductRepository productRepo;
    private final RefBusinessSegmentRepository segmentRepo; // Injected
    private final RefBusinessSubSegmentRepository subSegmentRepo; // Injected
    private final EmployeeMatrixAssignmentRepository matrixRepo;

    @Transactional(readOnly = true)
    public ResolutionResponse resolveUsers(ResolutionRequest request) {
        // 1. Resolve Region Hierarchy
        RefRegion targetRegion = regionRepo.findByRegionName(request.getRegion())
                .orElseThrow(() -> new IllegalArgumentException("Region not found: " + request.getRegion()));
        
        List<Long> regionScopeIds = parseRegionPath(targetRegion.getPath());
        
        // 2. Resolve Product & Segment & SubSegment
        RefProduct targetProduct = null;
        if (request.getProduct() != null) {
            targetProduct = productRepo.findByProductName(request.getProduct())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProduct()));
        }

        RefBusinessSegment targetSegment = null;
        if (request.getBusinessSegment() != null) {
            targetSegment = segmentRepo.findBySegmentName(request.getBusinessSegment())
                    .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + request.getBusinessSegment()));
        }

        RefBusinessSubSegment targetSubSegment = null;
        if (request.getBusinessSubSegment() != null) {
            targetSubSegment = subSegmentRepo.findBySubSegmentName(request.getBusinessSubSegment())
                    .orElseThrow(() -> new IllegalArgumentException("SubSegment not found: " + request.getBusinessSubSegment()));
        }

        // 3. Query Matrix (Find all candidates in the region chain)
        List<EmployeeMatrixAssignment> candidates = matrixRepo.findByRoleRoleCodeAndScopeRegionRegionIdIn(
                request.getRole(), regionScopeIds);

        // 4. Filter & Score
        RefProduct finalProduct = targetProduct;
        RefBusinessSegment finalSegment = targetSegment;
        RefBusinessSubSegment finalSubSegment = targetSubSegment;

        Optional<EmployeeMatrixAssignment> bestMatch = candidates.stream()
                .filter(a -> isScopeMatch(a, finalProduct, finalSegment, finalSubSegment))
                .filter(a -> isAmountCovered(a, request.getAmount(), request.getContext()))
                .max(Comparator.comparing(this::calculateSpecificity)); // Max score wins

        if (bestMatch.isPresent()) {
            EmployeeMatrixAssignment assignment = bestMatch.get();
            String reason = String.format("Matched User %s (Role: %s, Region: %s, Limit: %s)", 
                    assignment.getEmployee().getFullName(), 
                    assignment.getRole().getRoleName(),
                    assignment.getScopeRegion().getRegionName(),
                    assignment.getApprovalLimit());
            
            return new ResolutionResponse(List.of(assignment.getEmployee().getEmployeeId()), reason);
        } else {
            return new ResolutionResponse(Collections.emptyList(), "No matching approver found for criteria.");
        }
    }

    private List<Long> parseRegionPath(String path) {
        // Path format: /1/5/20/ -> [1, 5, 20]
        return Arrays.stream(path.split("/"))
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    private boolean isScopeMatch(EmployeeMatrixAssignment assignment, 
                                 RefProduct targetProduct, 
                                 RefBusinessSegment targetSegment,
                                 RefBusinessSubSegment targetSubSegment) {
        
        // Case A: Assignment is Global (No Scope) -> Matches Everything
        if (assignment.getScopeProduct() == null && 
            assignment.getScopeSegment() == null && 
            assignment.getScopeSubSegment() == null) {
            return true;
        }

        // Case B: Product Match (Highest Specificity)
        if (assignment.getScopeProduct() != null) {
            return targetProduct != null && assignment.getScopeProduct().getProductId().equals(targetProduct.getProductId());
        }

        // Case C: SubSegment Match
        if (assignment.getScopeSubSegment() != null) {
             return targetSubSegment != null && assignment.getScopeSubSegment().getSubSegmentId().equals(targetSubSegment.getSubSegmentId());
        }

        // Case D: Segment Match
        if (assignment.getScopeSegment() != null) {
            // Match if Target Segment matches Assignment Segment
            if (targetSegment != null && assignment.getScopeSegment().getSegmentId().equals(targetSegment.getSegmentId())) {
                return true;
            }
            // Match if Target SubSegment belongs to Assignment Segment
            if (targetSubSegment != null && targetSubSegment.getBusinessSegment().getSegmentId().equals(assignment.getScopeSegment().getSegmentId())) {
                return true;
            }
            // Match if Target Product belongs to Assignment Segment
             if (targetProduct != null && targetProduct.getSegment().getSegmentId().equals(assignment.getScopeSegment().getSegmentId())) {
                return true;
            }
        }

        return false;
    }

    private boolean isAmountCovered(EmployeeMatrixAssignment assignment, BigDecimal requestAmount, Map<String, Object> context) {
        if (requestAmount == null || BigDecimal.ZERO.equals(requestAmount)) {
            return true; // No amount check needed
        }
        
        // TODO: Currency Conversion (Assuming request is in Base/Assignment Currency for MVP)
        BigDecimal limit = assignment.getApprovalLimit();
        if (limit == null) return false;

        return limit.compareTo(requestAmount) >= 0;
    }

    private int calculateSpecificity(EmployeeMatrixAssignment a) {
        int score = 0;
        
        // Product Specificity
        if (a.getScopeProduct() != null) score += 100;       // Exact Product match
        else if (a.getScopeSubSegment() != null) score += 75; // SubSegment match
        else if (a.getScopeSegment() != null) score += 50;   // Segment match
        
        // Region Specificity (Deeper is better)
        score += a.getScopeRegion().getPath().length(); 
        
        return score;
    }
}
