package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.dto.EmployeeDto;
import com.atomskills.atombackend.services.EmployeeQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeQueryService service;

    public EmployeeController(EmployeeQueryService service) {
        this.service = service;
    }

    /**
     * GET /api/employees?lastName=Иванов&firstName=Пётр&page=0&size=20&sort=tabel,asc
     * Возвращает рабочих с перечнем квалификаций (вид работ + уровень профессионализма).
     */
    @GetMapping
    public Page<EmployeeDto> list(
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String firstName,
            @PageableDefault(size = 20, sort = "tabel", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.pageWithQualifications(pageable, lastName, firstName);
    }
}