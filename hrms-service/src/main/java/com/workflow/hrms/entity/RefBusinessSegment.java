package com.workflow.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "ref_business_segment")
public class RefBusinessSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long segmentId;

    private String segmentName;

    @ManyToOne
    @JoinColumn(name = "parent_segment_id")
    private RefBusinessSegment parentSegment;
}
