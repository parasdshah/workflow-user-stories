package com.workflow.service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "screen_definition")
public class ScreenDefinition {

    @Id
    @Column(nullable = false, unique = true)
    private String screenCode;

    private String description;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String layoutJson; // Storing UI structure as JSON

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
