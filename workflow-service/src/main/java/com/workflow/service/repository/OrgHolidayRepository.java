package com.workflow.service.repository;

import com.workflow.service.entity.OrgHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrgHolidayRepository extends JpaRepository<OrgHoliday, Long> {
    List<OrgHoliday> findByRegion(String region);

    boolean existsByDateAndRegion(LocalDate date, String region);
}
