package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Направленная связь. Статус берётся у объекта-источника для ресурса связи. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependencyStatusDto {
    private Long idDependence;
    private String resourceType;
    private Long sourceId;
    private Long dependentId;
    private StatusDto status;
}
