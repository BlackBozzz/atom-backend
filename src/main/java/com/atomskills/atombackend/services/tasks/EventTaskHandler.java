package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.EventTask;
import com.atomskills.atombackend.entity.Edifice;
import com.atomskills.atombackend.entity.Event;
import com.atomskills.atombackend.entity.EventStatus;
import com.atomskills.atombackend.repository.EdificeRepository;
import com.atomskills.atombackend.repository.EventRepository;
import com.atomskills.atombackend.repository.EventStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EventTaskHandler {
    private final EventRepository repository;
    private final EdificeRepository edifices;
    private final EventStatusRepository statuses;

    @ArgentQueueHandler("external-event")
    public void handle(EventTask t) {
        Edifice edifice = edifices.findById(t.getIdEdifice())
                .orElseThrow(() -> new IllegalStateException(
                        "edifice " + t.getIdEdifice() + " ещё не синхронизирован"));
        EventStatus status = statuses.findById(t.getIdStatus())
                .orElseThrow(() -> new IllegalStateException(
                        "event_status " + t.getIdStatus() + " ещё не синхронизирован"));

        Event event = repository.findById(t.getIdEvent()).orElseGet(Event::new);
        event.setIdEvent(t.getIdEvent());
        event.setEdifice(edifice);
        event.setResourceType(t.getResourceType());
        event.setOccurredAt(t.getOccurredAt());
        event.setStatus(status);
        event.setSyncedAt(Instant.now());
        repository.save(event);
    }
}
