package com.workflow.service.repository;

import com.workflow.service.entity.ScreenDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenDefinitionRepository extends JpaRepository<ScreenDefinition, String> {
}
