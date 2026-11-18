package com.atomskills.atombackend.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventTask {
    private Long idEvent;
    private Long idEdifice;
    private String resourceType;
    private LocalDateTime occurredAt;
    private Long idStatus;
}
