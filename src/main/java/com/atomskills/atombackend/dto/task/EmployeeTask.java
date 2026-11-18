package com.atomskills.atombackend.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTask {
    private Long tabel;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate startDateExperience;
}
