package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Направленная связь. Статус берётся у объекта-источника для ресурса связи. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependencyStatusDto {
    private Integer idDependence;
    private String resourceType;
    private Integer sourceId;
    private Integer dependentId;
    private StatusDto status;
}
