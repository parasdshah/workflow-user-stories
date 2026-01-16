package com.workflow.hrms.repository;

import com.workflow.hrms.entity.EmployeeMatrixAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeMatrixAssignmentRepository extends JpaRepository<EmployeeMatrixAssignment, Long> {
    List<EmployeeMatrixAssignment> findByRoleRoleCodeAndScopeRegionRegionIdIn(String roleCode, List<Long> regionIds);
    List<EmployeeMatrixAssignment> findByEmployeeEmployeeId(String employeeId);
}
