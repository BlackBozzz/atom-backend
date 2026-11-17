package com.atomskills.atombackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "edifice_dependency")
@Getter
@Setter
@NoArgsConstructor
public class EdificeDependency {

    @Id
    @Column(name = "id_dependence", nullable = false)
    private Integer idDependence;

    @Column(name = "resource_type", nullable = false, length = 15)
    private String resourceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_edifice_source", nullable = false)
    private Edifice source;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_edifice_dependent", nullable = false)
    private Edifice dependent;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();
}