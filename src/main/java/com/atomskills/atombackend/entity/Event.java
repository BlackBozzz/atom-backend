package com.atomskills.atombackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Data
public class Event {

    @Id
    @Column(name = "id_event", nullable = false)
    private Integer idEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_edifice", nullable = false)
    private Edifice edifice;

    @Column(name = "resource_type", nullable = false, length = 15)
    private String resourceType;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_status", nullable = false)
    private EventStatus status;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();
}