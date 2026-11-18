package com.atomskills.atombackend.dto.caclulate;

import com.atomskills.atombackend.dto.BrigadeDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BrigadesResponseDto {
    private List<BrigadeDto> brigades;
}
