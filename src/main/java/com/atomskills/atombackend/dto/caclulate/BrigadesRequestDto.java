package com.atomskills.atombackend.dto.caclulate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BrigadesRequestDto {
    private List<BrigadeRequestItem> brigades;

    @Getter
    @Setter
    public static class BrigadeRequestItem {
        @JsonProperty("brigade_id")
        private Long brigadeId;

        private List<Long> employees;

        @JsonProperty("event_order")
        private List<Long> eventOrder;
    }
}
