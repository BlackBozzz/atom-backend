package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.dto.EventStatusDto;
import com.atomskills.atombackend.services.EventStatusQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/event-statuses")
public class EventStatusController {

    private final EventStatusQueryService service;

    public EventStatusController(EventStatusQueryService service) {
        this.service = service;
    }

    @GetMapping
    public Page<EventStatusDto> list(
            @PageableDefault(size = 20, sort = "idStatus") Pageable pageable) {
        return service.page(pageable);
    }
}