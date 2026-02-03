package com.workflow.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "ref_business_sub_segment")
public class RefBusinessSubSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subSegmentId;

    private String subSegmentName;

    @ManyToOne
    @JoinColumn(name = "business_segment_id")
    private RefBusinessSegment businessSegment;
}
