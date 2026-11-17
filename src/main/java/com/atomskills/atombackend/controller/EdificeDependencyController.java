package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.dto.EdificeDependencyDto;
import com.atomskills.atombackend.services.EdificeDependencyQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/edifice-dependencies")
public class EdificeDependencyController {

    private final EdificeDependencyQueryService service;

    public EdificeDependencyController(EdificeDependencyQueryService service) {
        this.service = service;
    }

    @GetMapping
    public Page<EdificeDependencyDto> list(
            @RequestParam(required = false) Integer idSource,
            @RequestParam(required = false) Integer idDependent,
            @PageableDefault(size = 20, sort = "idDependence") Pageable pageable) {
        return service.page(pageable, idSource, idDependent);
    }
}