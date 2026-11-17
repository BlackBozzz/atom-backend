package com.atomskills.atombackend.dto;

public record EdificeTask(
        int idEdifice,
        String name,
        String[] resourceType,
        String district,
        String street,
        String houseNumber
) { }