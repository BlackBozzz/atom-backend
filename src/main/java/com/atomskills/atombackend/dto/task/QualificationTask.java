package com.atomskills.atombackend.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualificationTask {
    private Integer idQualification;
    private Long idEmployee;
    private String resourceType;
    private BigDecimal proff;
}