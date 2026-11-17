package com.atomskills.atombackend.dto;


import java.time.Instant;
import java.time.LocalDateTime;

public record LastEventDto(
        Integer idEvent,
        Integer idEdifice,
        String edificeName,
        String resourceType,
        LocalDateTime occurredAt,
        Short idStatus,
        String statusName,
        Instant syncedAt
) {}