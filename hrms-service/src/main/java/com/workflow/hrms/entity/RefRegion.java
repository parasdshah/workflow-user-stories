package com.workflow.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "ref_region")
public class RefRegion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long regionId;

    private String regionName;

    @Enumerated(EnumType.STRING)
    private RegionType regionType;

    private String pincode; // Only for BRANCH

    @ManyToOne
    @JoinColumn(name = "parent_region_id")
    private RefRegion parentRegion;

    // Materialized Path: /1/5/20/
    private String path;

    public enum RegionType {
        GLOBAL, CONTINENT, COUNTRY, STATE, CITY, BRANCH
    }
}
