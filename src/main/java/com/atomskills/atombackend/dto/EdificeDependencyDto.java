package com.atomskills.atombackend.dto;

import lombok.Data;

import java.time.Instant;

public record EdificeDependencyDto(
        Integer idDependence,
        String resourceType,
        EdificeDto source,
        EdificeDto dependent,
        Instant syncedAt
) { }