package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.EdificeTask;
import com.atomskills.atombackend.entity.Edifice;
import com.atomskills.atombackend.repository.EdificeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EdificeTaskHandler {
    private final EdificeRepository repository;

    @ArgentQueueHandler("external-edifice")
    public void handle(EdificeTask t) {
        Edifice e = repository.findById(t.getIdEdifice()).orElseGet(Edifice::new);
        e.setIdEdifice(t.getIdEdifice());
        e.setName(t.getName());
        e.setResourceType(t.getResourceType());
        e.setDistrict(t.getDistrict());
        e.setStreet(t.getStreet());
        e.setHouseNumber(t.getHouseNumber());
        e.setSyncedAt(Instant.now());
        repository.save(e);
    }
}