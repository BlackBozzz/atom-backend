package com.atomskills.atombackend.entity;

import com.argent.servicecommon.dictionary.annotation.ArgentDataTable;
import com.atomskills.atombackend.dto.EventDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
@ArgentDataTable(name = "События", dtoClass = EventDto.class)
public class Event {

    @Id
    @Column(name = "id_event", nullable = false)
    private Long idEvent;

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