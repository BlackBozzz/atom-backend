package com.atomskills.atombackend.dto;

import lombok.Data;

import java.time.Instant;

public record EdificeDto(
        Integer idEdifice,
        String name,
        String[] resourceType,
        String district,
        String street,
        String houseNumber,
        Instant syncedAt
) { }