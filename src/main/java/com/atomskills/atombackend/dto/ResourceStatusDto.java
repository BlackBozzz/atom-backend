package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Статус одного ресурса конкретного объекта. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceStatusDto {
    private String resourceType;
    private StatusDto status;
}
