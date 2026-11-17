package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdificeTask {
    private int idEdifice;
    private String name;
    private String[] resourceType;
    private String district;
    private String street;
    private String houseNumber;
}
