package com.atomskills.atombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Последний зарегистрированный статус ресурса и время события. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusDto {
    private Short idStatus;
    private String name;
    private LocalDateTime occurredAt;
}
