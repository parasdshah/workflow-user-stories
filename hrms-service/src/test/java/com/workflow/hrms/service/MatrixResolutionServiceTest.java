package com.workflow.hrms.service;

import com.workflow.hrms.dto.ResolutionRequest;
import com.workflow.hrms.dto.ResolutionResponse;
import com.workflow.hrms.entity.*;
import com.workflow.hrms.repository.EmployeeMatrixAssignmentRepository;
import com.workflow.hrms.repository.RefProductRepository;
import com.workflow.hrms.repository.RefRegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatrixResolutionServiceTest {

    @Mock
    private RefRegionRepository regionRepo;
    @Mock
    private RefProductRepository productRepo;
    @Mock
    private EmployeeMatrixAssignmentRepository matrixRepo;

    @InjectMocks
    private MatrixResolutionService service;

    private RefRegion mumbai;
    private RefProduct homeLoan;
    private EmployeeMatrixAssignment assignmentA;

    @BeforeEach
    void setUp() {
        // Mock Region
        mumbai = new RefRegion();
        mumbai.setRegionName("Mumbai");
        mumbai.setPath("/1/5/20/50/"); // Global/Asia/India/Mumbai
        
        // Mock Product
        homeLoan = new RefProduct();
        homeLoan.setProductName("Home Loan");
        homeLoan.setProductId(100L);
        RefBusinessSegment retail = new RefBusinessSegment();
        retail.setSegmentId(10L);
        homeLoan.setSegment(retail);

        // Mock Assignment (Correct Limit)
        EmployeeMaster userA = new EmployeeMaster();
        userA.setEmployeeId("USER_A");
        userA.setFullName("User A");
        
        RoleMaster role = new RoleMaster();
        role.setRoleName("Manager");

        assignmentA = new EmployeeMatrixAssignment();
        assignmentA.setEmployee(userA);
        assignmentA.setRole(role);
        assignmentA.setScopeRegion(mumbai);
        assignmentA.setApprovalLimit(new BigDecimal("100000")); // Limit 100k
    }

    @Test
    void testResolveUsers_Success() {
        // Given
        ResolutionRequest req = new ResolutionRequest();
        req.setRole("APPROVER");
        req.setRegion("Mumbai");
        req.setProduct("Home Loan");
        req.setAmount(new BigDecimal("50000")); // < 100k

        when(regionRepo.findByRegionName("Mumbai")).thenReturn(Optional.of(mumbai));
        when(productRepo.findByProductName("Home Loan")).thenReturn(Optional.of(homeLoan));
        // Mock finding candidates
        when(matrixRepo.findByRoleRoleCodeAndScopeRegionRegionIdIn(eq("APPROVER"), anyList()))
                .thenReturn(List.of(assignmentA));

        // When
        ResolutionResponse response = service.resolveUsers(req);

        // Then
        assertFalse(response.getUserIds().isEmpty());
        assertEquals("USER_A", response.getUserIds().get(0));
    }

    @Test
    void testResolveUsers_Fail_LimitExceeded() {
        // Given
        ResolutionRequest req = new ResolutionRequest();
        req.setRole("APPROVER");
        req.setRegion("Mumbai");
        req.setProduct("Home Loan");
        req.setAmount(new BigDecimal("200000")); // > 100k

        when(regionRepo.findByRegionName("Mumbai")).thenReturn(Optional.of(mumbai));
        when(productRepo.findByProductName("Home Loan")).thenReturn(Optional.of(homeLoan));
        when(matrixRepo.findByRoleRoleCodeAndScopeRegionRegionIdIn(eq("APPROVER"), anyList()))
                .thenReturn(List.of(assignmentA));

        // When
        ResolutionResponse response = service.resolveUsers(req);

        // Then
        assertTrue(response.getUserIds().isEmpty(), "Should fail due to limit");
    }
}
