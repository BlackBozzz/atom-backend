package com.atomskills.atombackend.dto;

import lombok.Data;

@Data
public class BaseDurationDto {
    private String resourceType;
    private Integer idEdifice;
    private Long avgResolutionSeconds; // interval преобразуем в секунды
}
