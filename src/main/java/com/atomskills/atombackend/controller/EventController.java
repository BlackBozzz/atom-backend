package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.dto.EventDto;
import com.atomskills.atombackend.dto.LastEventDto;
import com.atomskills.atombackend.services.AtomEventQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/atom-events")
public class EventController {

    private final AtomEventQueryService service;

    public EventController(AtomEventQueryService service) {
        this.service = service;
    }

    @GetMapping
    public Page<EventDto> list(
            @RequestParam(required = false) Integer idEdifice,
            @RequestParam(required = false) Short idStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.page(pageable, idEdifice, idStatus, from, to);
    }

    /**
     * GET /api/legacy/atom-events/latest?idEdifice=123
     * Возвращает последнее (текущее) событие на объекте и его статус.
     * 404 — если у объекта нет событий.
     */
    @GetMapping("/latest")
    public ResponseEntity<LastEventDto> latest(@RequestParam Integer idEdifice) {
        return service.findLatestByEdifice(idEdifice)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}