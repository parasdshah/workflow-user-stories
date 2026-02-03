package com.workflow.hrms.repository;

import com.workflow.hrms.entity.RefBusinessSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefBusinessSegmentRepository extends JpaRepository<RefBusinessSegment, Long> {
    java.util.Optional<RefBusinessSegment> findBySegmentName(String segmentName);
}
