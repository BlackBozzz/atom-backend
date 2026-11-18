package com.atomskills.atombackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class BrigadeDto {
    @JsonProperty("brigade_id")
    private Long brigadeId;

    private List<EmployeeDto> employees;

    @JsonProperty("event_order")
    private List<EventDto> eventOrder;
}