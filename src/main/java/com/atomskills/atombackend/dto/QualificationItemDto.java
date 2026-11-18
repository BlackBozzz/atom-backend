package com.atomskills.atombackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QualificationItemDto {
    private Long tabel;                 // для группировки
    private Integer idQualification;
    private String resourceType;
    private BigDecimal proff;
}