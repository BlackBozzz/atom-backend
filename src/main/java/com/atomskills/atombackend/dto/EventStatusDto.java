package com.atomskills.atombackend.dto;

import lombok.Data;


public record EventStatusDto(
        Short idStatus,
        String name
) { }