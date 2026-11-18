package com.atomskills.atombackend.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeResolveTask {
    private Long idTime;
    private String resourceType;
    private String avgResolutionTime;
    private Long idEdifice;
}