package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.EventStatusTask;
import com.atomskills.atombackend.entity.EventStatus;
import com.atomskills.atombackend.repository.EventStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EventStatusTaskHandler {
    private final EventStatusRepository repository;

    @ArgentQueueHandler("external-event-status")
    public void handle(EventStatusTask t) {
        EventStatus s = repository.findById(t.idStatus()).orElseGet(EventStatus::new);
        s.setIdStatus(t.idStatus());
        s.setName(t.name());
        s.setSyncedAt(Instant.now());
        repository.save(s);
    }
}
