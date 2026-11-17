package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventTask {
    private int idEvent;
    private int idEdifice;
    private String resourceType;
    private LocalDateTime occurredAt;
    private short idStatus;
}
