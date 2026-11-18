package com.atomskills.atombackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepairCandidateDto {
    private Long idEvent;
    private Long idEdifice;
    private String resourceType;
    private LocalDateTime occurredAt;
    private Long idStatus;
    private String statusName; // Из справочника event_statuses
}
