package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.dto.EdificeStateDto;
import com.atomskills.atombackend.services.EdificeStatusQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/edifices/status")
public class EdificeCurrentStatusController {

    private final EdificeStatusQueryService service;

    public EdificeCurrentStatusController(EdificeStatusQueryService service) {
        this.service = service;
    }

    @GetMapping("/current")
    public List<EdificeStateDto> current() {
        return service.findCurrent();
    }
}
