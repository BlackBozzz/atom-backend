package com.atomskills.atombackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "qualification")
@Getter
@Setter
public class Qualification {
    @Id
    @Column(name = "id_qualification")
    private Long idQualification;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_employee")
    private Employee employee;
    @Column(name = "resource_type") private String resourceType;
    @Column(name = "proff") private BigDecimal proff;
    @Column(name = "synced_at") private Instant syncedAt;
    // getters/setters
}
