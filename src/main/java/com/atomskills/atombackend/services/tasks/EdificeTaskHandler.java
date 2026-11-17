package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.EdificeTask;
import com.atomskills.atombackend.entity.Edifice;
import com.atomskills.atombackend.repository.EdificeRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EdificeTaskHandler {
    private final EdificeRepository repository;
    public EdificeTaskHandler(EdificeRepository repository) {
        this.repository = repository;
    }

    @ArgentQueueHandler("external-edifice")
    public void handle(EdificeTask t) {
        Edifice e = repository.findById(t.idEdifice()).orElseGet(Edifice::new);
        e.setIdEdifice(t.idEdifice());
        e.setName(t.name());
        e.setResourceType(t.resourceType());
        e.setDistrict(t.district());
        e.setStreet(t.street());
        e.setHouseNumber(t.houseNumber());
        e.setSyncedAt(Instant.now());
        repository.save(e);
    }
}