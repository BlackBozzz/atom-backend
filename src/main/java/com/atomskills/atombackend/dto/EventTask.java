package com.atomskills.atombackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public record EventTask(
        int idEvent,
        int idEdifice,
        String resourceType,
        LocalDateTime occurredAt,
        short idStatus
) { }