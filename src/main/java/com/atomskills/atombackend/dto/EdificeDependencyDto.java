package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdificeDependencyDto {
    private Long idDependence;
    private String resourceType;
    private EdificeDto source;
    private EdificeDto dependent;
    private Instant syncedAt;
}
