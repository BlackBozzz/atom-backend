package com.atomskills.atombackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QualificationItemDto {
    private Long tabel;                 // для группировки
    private Long idQualification;
    private String resourceType;
    private BigDecimal proff;
}