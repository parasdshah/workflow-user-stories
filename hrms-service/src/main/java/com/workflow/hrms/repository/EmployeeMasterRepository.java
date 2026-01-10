package com.workflow.hrms.repository;

import com.workflow.hrms.entity.EmployeeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeMasterRepository extends JpaRepository<EmployeeMaster, String> {}
