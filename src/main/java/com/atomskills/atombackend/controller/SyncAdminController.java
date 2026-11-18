package com.atomskills.atombackend.controller;

import com.atomskills.atombackend.services.SyncAdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncAdminController {
    private final SyncAdminService service;

    public SyncAdminController(SyncAdminService service) {
        this.service = service;
    }

    /** Полная очистка всех синхронизированных данных и сброс всех курсоров. */
    @DeleteMapping("/all")
    public void clearAll() {
        service.clearAll();
    }
}
