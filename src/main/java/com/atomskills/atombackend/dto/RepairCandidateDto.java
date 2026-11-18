package com.atomskills.atombackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepairCandidateDto {
    private Integer idEvent;
    private Integer idEdifice;
    private String resourceType;
    private LocalDateTime occurredAt;
    private Integer idStatus;
    private String statusName; // Из справочника event_statuses
}
