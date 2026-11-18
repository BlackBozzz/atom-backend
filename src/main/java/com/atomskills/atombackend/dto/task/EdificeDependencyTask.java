package com.atomskills.atombackend.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdificeDependencyTask {
    private Long idDependence;
    private String resourceType;
    private Long idEdificeSource;
    private Long idEdificeDependent;
}
