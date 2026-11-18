package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.dto.EdificeStateDto;
import com.atomskills.atombackend.services.EdificeStatusQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/edifices/status")
public class EdificeStatusAtController {

    private final EdificeStatusQueryService service;

    public EdificeStatusAtController(EdificeStatusQueryService service) {
        this.service = service;
    }

    // Время без часового пояса: соответствует типу event.occurred_at (timestamp).
    @GetMapping("/at")
    public List<EdificeStateDto> at(
            @RequestParam("at")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return service.findAt(at);
    }
}
