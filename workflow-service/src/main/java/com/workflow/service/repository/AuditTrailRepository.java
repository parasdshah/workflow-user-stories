package com.workflow.service.repository;

import com.workflow.service.entity.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long>, JpaSpecificationExecutor<AuditTrail> {
    List<AuditTrail> findByEntityNameAndEntityIdOrderByChangedAtDesc(String entityName, String entityId);
}
