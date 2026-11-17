package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LastEventDto {
    private Integer idEvent;
    private Integer idEdifice;
    private String edificeName;
    private String resourceType;
    private LocalDateTime occurredAt;
    private Short idStatus;
    private String statusName;
    private Instant syncedAt;
}
