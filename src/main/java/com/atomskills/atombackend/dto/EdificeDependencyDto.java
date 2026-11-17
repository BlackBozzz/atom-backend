package com.atomskills.atombackend.dto;

import com.atomskills.atombackend.entity.Edifice;

import java.time.Instant;

public record EdificeDependencyDto(
        Integer idDependence,
        String resourceType,
        EdificeDto source,
        EdificeDto dependent,
        Instant syncedAt
) { }