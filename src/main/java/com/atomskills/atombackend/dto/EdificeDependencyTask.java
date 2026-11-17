package com.atomskills.atombackend.dto;

public record EdificeDependencyTask(
        int idDependence,
        String resourceType,
        int idEdificeSource,
        int idEdificeDependent
) { }