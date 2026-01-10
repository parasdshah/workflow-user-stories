package com.workflow.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "ref_product")
public class RefProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private String productName;

    @ManyToOne
    @JoinColumn(name = "segment_id", nullable = false)
    private RefBusinessSegment segment;
}
