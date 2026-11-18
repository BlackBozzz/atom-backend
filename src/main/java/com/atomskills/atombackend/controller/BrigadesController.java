package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.dto.caclulate.BrigadesResponseDto;
import com.atomskills.atombackend.services.BrigadeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brigades")
@RequiredArgsConstructor
public class BrigadesController {
    private final BrigadeQueryService service;

    @GetMapping
    public BrigadesResponseDto list() {
        return service.getAll();
    }
}
