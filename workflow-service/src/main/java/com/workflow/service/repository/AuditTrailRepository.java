package com.workflow.service.repository;

import com.workflow.service.entity.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    List<AuditTrail> findByEntityNameAndEntityIdOrderByChangedAtDesc(String entityName, String entityId);
}
