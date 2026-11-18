package com.atomskills.atombackend.entity;

import com.argent.servicecommon.dictionary.annotation.ArgentDataTable;
import com.atomskills.atombackend.dto.EventStatusDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "event_status")
@Getter
@Setter
@NoArgsConstructor
@ArgentDataTable(name = "Статусы событий", dtoClass = EventStatusDto.class)
public class EventStatus {

    @Id
    @Column(name = "id_status", nullable = false)
    private Long idStatus;

    @Column(nullable = false, length = 20, unique = true)
    private String name;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();
}