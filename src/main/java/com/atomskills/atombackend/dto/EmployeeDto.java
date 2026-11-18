package com.atomskills.atombackend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class EmployeeDto {
    private Long tabel;
    private String lastName;
    private String firstName;
    private String middleName;
    private LocalDate startDateExperience;
    private List<QualificationItemDto> qualifications = new ArrayList<>();
}