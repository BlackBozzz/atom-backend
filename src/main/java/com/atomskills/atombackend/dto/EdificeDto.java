package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdificeDto {
    private Long idEdifice;
    private String name;
    private String[] resourceType;
    private String district;
    private String street;
    private String houseNumber;
    private Instant syncedAt;
}
