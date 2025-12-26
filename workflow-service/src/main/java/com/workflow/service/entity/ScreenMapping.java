package com.workflow.service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "screen_mapping")
public class ScreenMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stageCode;

    @Column(nullable = false)
    private String screenCode;

    @Enumerated(EnumType.STRING)
    private AccessType accessType;

    public enum AccessType {
        EDITABLE,
        READ_ONLY
    }
}
