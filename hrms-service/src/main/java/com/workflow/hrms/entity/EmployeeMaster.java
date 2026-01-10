package com.workflow.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "employee_master")
public class EmployeeMaster {
    @Id
    private String employeeId; // e.g., "EMP123"

    private String fullName;
    private String email;

    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;

    @ManyToOne
    @JoinColumn(name = "base_location_id")
    private RefRegion baseLocation;

    public enum EmployeeStatus {
        ACTIVE, TERMINATED, ON_LEAVE
    }
}
