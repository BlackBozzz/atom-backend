package com.atomskills.atombackend.dto;

import java.time.LocalDateTime;

public record EventTask(
        int idEvent,
        int idEdifice,
        String resourceType,
        LocalDateTime occurredAt,
        short idStatus
) { }