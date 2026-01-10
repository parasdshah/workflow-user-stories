package com.workflow.hrms.repository;

import com.workflow.hrms.entity.RoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleMasterRepository extends JpaRepository<RoleMaster, String> {}
