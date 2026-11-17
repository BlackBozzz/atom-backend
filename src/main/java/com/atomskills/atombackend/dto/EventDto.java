package com.atomskills.atombackend.dto;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Плоский DTO: id-шники родителей + их читаемые имена.
 * Никаких @ManyToOne — иначе Jackson полезет в LAZY и упадёт.
 */
public record EventDto(
        Integer idEvent,
        Integer idEdifice,
        String edificeName,
        String resourceType,
        LocalDateTime occurredAt,
        Short idStatus,
        String statusName,
        Instant syncedAt
) { }