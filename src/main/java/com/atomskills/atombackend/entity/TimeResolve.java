package com.atomskills.atombackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "time_resolve")
@Getter
@Setter
public class TimeResolve {
    @Id
    @Column(name = "id_time")
    private Long idTime;
    @Column(name = "resource_type")
    private String resourceType;
    @Column(name = "avg_resolution_time", length = 40)
    private String avgResolutionTime;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_edifice")
    private Edifice edifice;
    @Column(name = "synced_at")
    private Instant syncedAt;
}