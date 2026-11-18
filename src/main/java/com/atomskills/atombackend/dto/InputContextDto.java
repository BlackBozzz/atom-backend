package com.atomskills.atombackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class InputContextDto {
    private List<WorkerDto> workers;
    private List<QualificationDto> qualifications;
    private List<RepairCandidateDto> repairs;
    private List<DependencyDto> dependencies;
    private List<BaseDurationDto> baseDurations;

    // Конфликтность обычно не хранится в этой схеме БД,
    // поэтому она останется null или пустым списком, если не передается отдельно
    private List<Object> conflictMatrix;
}