package com.atomskills.atombackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QualificationDto {
    private Integer tabel;
    private String resourceType;
    private BigDecimal proff;
}
