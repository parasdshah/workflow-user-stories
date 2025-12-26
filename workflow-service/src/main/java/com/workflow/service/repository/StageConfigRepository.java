package com.workflow.service.repository;

import com.workflow.service.entity.StageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StageConfigRepository extends JpaRepository<StageConfig, Long> {
    List<StageConfig> findByWorkflowCodeOrderBySequenceOrderAsc(String workflowCode);
    Optional<StageConfig> findByWorkflowCodeAndStageCode(String workflowCode, String stageCode);
    boolean existsByWorkflowCodeAndStageCode(String workflowCode, String stageCode);
}
