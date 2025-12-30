package com.workflow.service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "module_master")
@Data
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_code", unique = true, nullable = false)
    private String moduleCode;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "description")
    private String description;
}
