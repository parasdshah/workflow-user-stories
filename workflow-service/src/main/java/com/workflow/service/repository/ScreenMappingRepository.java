package com.workflow.service.repository;

import com.workflow.service.entity.ScreenMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenMappingRepository extends JpaRepository<ScreenMapping, Long> {
    List<ScreenMapping> findByStageCode(String stageCode);

    Optional<ScreenMapping> findByStageCodeAndScreenCode(String stageCode, String screenCode);

    void deleteByStageCode(String stageCode);
}
