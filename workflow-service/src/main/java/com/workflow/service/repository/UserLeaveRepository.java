package com.workflow.service.repository;

import com.workflow.service.entity.UserLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLeaveRepository extends JpaRepository<UserLeave, Long> {

    // Find active leaves for a user that overlap with the current time
    @Query("SELECT l FROM UserLeave l WHERE l.userId = :userId AND l.active = true AND :checkTime BETWEEN l.fromDate AND l.toDate")
    Optional<UserLeave> findActiveLeave(String userId, LocalDateTime checkTime);

    List<UserLeave> findByUserId(String userId);
}
