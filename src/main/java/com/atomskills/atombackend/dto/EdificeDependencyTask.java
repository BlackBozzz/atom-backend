package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdificeDependencyTask {
    private int idDependence;
    private String resourceType;
    private int idEdificeSource;
    private int idEdificeDependent;
}
