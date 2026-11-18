package com.atomskills.atombackend.dto;

import lombok.Data;

@Data
public class DependencyDto {
    private String resourceType;
    private Long idEdificeDependent;
    private Long idEdificeSource;
}
