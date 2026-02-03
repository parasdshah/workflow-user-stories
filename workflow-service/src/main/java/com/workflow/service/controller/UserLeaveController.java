package com.workflow.service.controller;

import com.workflow.service.entity.UserLeave;
import com.workflow.service.repository.UserLeaveRepository;
import com.workflow.service.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user-leaves")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserLeaveController {

    private final UserLeaveRepository leaveRepository;
    private final CalendarService calendarService;

    @GetMapping("/active/{userId}")
    public ResponseEntity<UserLeave> getActiveLeave(@PathVariable String userId) {
        UserLeave leave = calendarService.getActiveLeave(userId);
        if (leave != null) {
            return ResponseEntity.ok(leave);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public List<UserLeave> getUserLeaves(@PathVariable String userId) {
        return leaveRepository.findByUserId(userId);
    }

    @PostMapping
    public UserLeave createLeave(@RequestBody UserLeave leave) {
        // Validation: Verify if substitute exists or if user is already OOO could be
        // added here
        if (leave.getFromDate() == null || leave.getToDate() == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        return leaveRepository.save(leave);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeave(@PathVariable Long id) {
        leaveRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
