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
    private final EmployeeMatrixAssignmentRepository matrixRepo;

    @Transactional(readOnly = true)
    public ResolutionResponse resolveUsers(ResolutionRequest request) {
        // 1. Resolve Region Hierarchy
        RefRegion targetRegion = regionRepo.findByRegionName(request.getRegion())
                .orElseThrow(() -> new IllegalArgumentException("Region not found: " + request.getRegion()));
        
        List<Long> regionScopeIds = parseRegionPath(targetRegion.getPath());
        
        // 2. Resolve Product & Segment
        RefProduct targetProduct = null;
        if (request.getProduct() != null) {
            targetProduct = productRepo.findByProductName(request.getProduct())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProduct()));
        }

        // 3. Query Matrix (Find all candidates in the region chain)
        List<EmployeeMatrixAssignment> candidates = matrixRepo.findByRoleRoleCodeAndScopeRegionRegionIdIn(
                request.getRole(), regionScopeIds);

        // 4. Filter & Score
        RefProduct finalProduct = targetProduct;
        Optional<EmployeeMatrixAssignment> bestMatch = candidates.stream()
                .filter(a -> isProductMatch(a, finalProduct))
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

    private boolean isProductMatch(EmployeeMatrixAssignment assignment, RefProduct targetProduct) {
        // Case A: Assignment is Global (No Segment/Product Scope) -> Matches Everything
        if (assignment.getScopeSegment() == null && assignment.getScopeProduct() == null) {
            return true;
        }

        if (targetProduct == null) {
            // Request has no product, but Assignment has scope. 
            // Strict mode: Only Generic Assignments match Generic Requests.
            return assignment.getScopeProduct() == null && assignment.getScopeSegment() == null;
        }

        // Case B: Assignment is Product Specific
        if (assignment.getScopeProduct() != null) {
            return assignment.getScopeProduct().getProductId().equals(targetProduct.getProductId());
        }

        // Case C: Assignment is Segment Specific
        if (assignment.getScopeSegment() != null) {
            // Match if Target Product belongs to Assignment's Segment
            return targetProduct.getSegment().getSegmentId().equals(assignment.getScopeSegment().getSegmentId());
        }

        return false;
    }

    private boolean isAmountCovered(EmployeeMatrixAssignment assignment, BigDecimal requestAmount, Map<String, Object> context) {
        if (requestAmount == null || BigDecimal.ZERO.equals(requestAmount)) {
            return true; // No amount check needed
        }
        
        // TODO: Currency Conversion (Assuming request is in Base/Assignment Currency for MVP)
        // In real world, check assignment.getCurrencyCode() vs request currency
        
        BigDecimal limit = assignment.getApprovalLimit();
        if (limit == null) return false;

        return limit.compareTo(requestAmount) >= 0;
    }

    private int calculateSpecificity(EmployeeMatrixAssignment a) {
        int score = 0;
        
        // Product Specificity
        if (a.getScopeProduct() != null) score += 100;       // Exact Product match (Home Loan Manager)
        else if (a.getScopeSegment() != null) score += 50;   // Segment match (Retail Head)
        
        // Region Specificity (Deeper is better)
        // We use path length as a proxy for depth
        score += a.getScopeRegion().getPath().length(); 
        
        return score;
    }
}
