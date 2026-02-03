package com.workflow.service.controller;

import com.workflow.service.entity.OrgHoliday;
import com.workflow.service.repository.OrgHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend access
public class HolidayController {

    private final OrgHolidayRepository holidayRepository;

    @GetMapping
    public List<OrgHoliday> getHolidays(@RequestParam(required = false) String region) {
        if (region != null) {
            return holidayRepository.findByRegion(region);
        }
        return holidayRepository.findAll();
    }

    @PostMapping
    public OrgHoliday createHoliday(@RequestBody OrgHoliday holiday) {
        return holidayRepository.save(holiday);
    }

    @PostMapping("/bulk")
    public List<OrgHoliday> createHolidays(@RequestBody List<OrgHoliday> holidays) {
        return holidayRepository.saveAll(holidays);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
