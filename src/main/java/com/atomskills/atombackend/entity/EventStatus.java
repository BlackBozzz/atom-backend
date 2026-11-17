package com.atomskills.atombackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Entity
@Table(name = "event_status")
@Getter
@Setter
public class EventStatus {

    @Id
    @Column(name = "id_status", nullable = false)
    private Short idStatus;

    @Column(nullable = false, length = 20, unique = true)
    private String name;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();
}