package com.workflow.hrms.repository;

import com.workflow.hrms.entity.RefRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefRegionRepository extends JpaRepository<RefRegion, Long> {
    List<RefRegion> findByPathStartingWith(String pathPrefix);
}
