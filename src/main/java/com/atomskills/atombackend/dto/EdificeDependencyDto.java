package com.atomskills.atombackend.dto;

import java.time.Instant;

public record EdificeDependencyDto(
        Integer idDependence,
        String resourceType,
        Integer idEdificeSource,
        String sourceName,
        Integer idEdificeDependent,
        String dependentName,
        Instant syncedAt
) { }