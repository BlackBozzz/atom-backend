package com.atomskills.atombackend.dto;

import lombok.Data;

@Data
public class DependencyDto {
    private String resourceType;
    private Integer idEdificeDependent;
    private Integer idEdificeSource;
}
