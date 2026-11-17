package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.dto.EdificeDto;
import com.atomskills.atombackend.services.EdificeQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/edifices")
public class EdificeController {

    private final EdificeQueryService service;

    public EdificeController(EdificeQueryService service) {
        this.service = service;
    }

    @GetMapping
    public Page<EdificeDto> list(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String street,
            @PageableDefault(size = 20, sort = "idEdifice") Pageable pageable) {
        return service.page(pageable, district, street);
    }
}