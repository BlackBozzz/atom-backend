package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.dto.caclulate.BrigadesResponseDto;
import com.atomskills.atombackend.services.PlannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
public class PlannerController {
    private final PlannerService plannerService;

    /**
     * Запускает процесс чтения данных из БД, оптимизации и возвращает готовый JSON.
     */
    @GetMapping(value = "/calculate")
    public BrigadesResponseDto calculatePlan() {
        return plannerService.calculateAndSavePlan();
    }
}