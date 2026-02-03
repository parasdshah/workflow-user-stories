package com.workflow.hrms.repository;

import com.workflow.hrms.entity.RefBusinessSubSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefBusinessSubSegmentRepository extends JpaRepository<RefBusinessSubSegment, Long> {
    Optional<RefBusinessSubSegment> findBySubSegmentName(String subSegmentName);
}
