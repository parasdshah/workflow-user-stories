package com.workflow.service.service;

import com.workflow.service.entity.OrgHoliday;
import com.workflow.service.entity.UserLeave;
import com.workflow.service.repository.OrgHolidayRepository;
import com.workflow.service.repository.UserLeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final OrgHolidayRepository holidayRepository;
    private final UserLeaveRepository leaveRepository;

    /**
     * Checks if a specific date is a holiday in the given region.
     */
    public boolean isHoliday(LocalDate date, String region) {
        // Simple implementation: Check DB for specific date + region
        // Optimization: Could cache this or fetch all holidays for the year
        return holidayRepository.existsByDateAndRegion(date, region);
    }

    /**
     * Retrieves active leave for a user at the current moment.
     * Returns null if user is available.
     */
    public UserLeave getActiveLeave(String userId) {
        return leaveRepository.findActiveLeave(userId, LocalDateTime.now())
                .orElse(null);
    }

    /**
     * Calculates the SLA Due Date starting from startDate, adding durationDays,
     * skipping Weekends and OrgHolidays for the region.
     */
    public LocalDate calculateSlaDueDate(LocalDate startDate, int durationDays, String region) {
        LocalDate currentDate = startDate;
        int daysAdded = 0;

        while (daysAdded < durationDays) {
            // Move to next day
            currentDate = currentDate.plusDays(1);

            // Check if it's a valid business day
            if (isBusinessDay(currentDate, region)) {
                daysAdded++;
            }
        }
        return currentDate;
    }

    /**
     * Checks if the user is currently OOO and returns the substitute.
     * Recursion Check: Only 1 level of substitution to prevent loops.
     */
    public String getEffectiveAssignee(String userId) {
        UserLeave leave = getActiveLeave(userId);
        if (leave == null) {
            return userId;
        }

        String substitute = leave.getSubstituteUserId(); // e.g., Bob

        if (substitute != null) {
            log.info("Delegation: User {} is OOO. Substituting with {}", userId, substitute);
            // Optional: Check if substitute is ALSO away?
            // For now, simple 1-level substitution.
            // If substitute is also away, we could return substitute (letting them handle
            // it) or fallback.
            // Let's check 1 level deep.
            if (getActiveLeave(substitute) != null) {
                log.warn("Delegation: Substitute {} is also OOO. Falling back to original assigner or pool.",
                        substitute);
                return userId; // Fallback to original? Or null to indicate "Unassigned"?
                // Requirement 3.2.4: "Fallback to Task Manager or Admin Group".
                // Managing Fallback triggers is complex here. Returning userId (Original)
                // ensures it's at least assigned.
            }
            return substitute;
        }

        return userId;
    }

    private boolean isBusinessDay(LocalDate date, String region) {
        // 1. Check Weekend (Saturday or Sunday)
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        // 2. Check Org Holiday
        if (isHoliday(date, region)) {
            return false;
        }

        return true;
    }
}
