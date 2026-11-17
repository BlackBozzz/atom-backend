package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Плоский DTO: id-шники родителей + их читаемые имена.
 * Никаких @ManyToOne — иначе Jackson полезет в LAZY и упадёт.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private Integer idEvent;
    private Integer idEdifice;
    private String edificeName;
    private String resourceType;
    private LocalDateTime occurredAt;
    private Short idStatus;
    private String statusName;
    private Instant syncedAt;
}
