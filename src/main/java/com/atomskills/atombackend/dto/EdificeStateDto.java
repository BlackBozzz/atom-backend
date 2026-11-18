package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Информация об объекте, его ресурсах и всех входящих/исходящих связях. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdificeStateDto {
    private Integer idEdifice;
    private String name;
    private List<String> resourceTypes;
    private String district;
    private String street;
    private String houseNumber;
    private List<ResourceStatusDto> resources;
    private List<DependencyStatusDto> dependencies;
}
