package com.atomskills.atombackend.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventStatusTask {
    private Long idStatus;
    private String name;
}
