package com.workflow.service.repository;

import com.workflow.service.entity.WorkflowMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WorkflowMasterRepository extends JpaRepository<WorkflowMaster, Long> {
    Optional<WorkflowMaster> findByWorkflowCode(String workflowCode);
    boolean existsByWorkflowCode(String workflowCode);
}
