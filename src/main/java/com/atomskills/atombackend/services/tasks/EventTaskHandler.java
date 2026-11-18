package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.EventBatchTask;
import com.atomskills.atombackend.dto.EventTask;
import com.atomskills.atombackend.entity.Event;
import com.atomskills.atombackend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EventTaskHandler {
    private final EventRepository repository;
    private final EdificeTaskHandler edifices;
    private final EventStatusTaskHandler statuses;

    @Transactional
    @ArgentQueueHandler("external-event-batch")
    public void handleBatch(EventBatchTask batch) {
        for (var task : batch.getItems()) {
            handle(task);
        }
    }

    @Transactional
    @ArgentQueueHandler("external-event")
    public void handle(EventTask task) {
        // Родители уже стоят раньше в FIFO, но возможна вставка во внешнюю БД прямо во время опроса.
        var edifice = edifices.ensurePresent(task.getIdEdifice());
        var status = statuses.ensurePresent(task.getIdStatus());

        Event event = repository.findById(task.getIdEvent()).orElseGet(Event::new);
        event.setIdEvent(task.getIdEvent());
        event.setEdifice(edifice);
        event.setResourceType(task.getResourceType());
        event.setOccurredAt(task.getOccurredAt());
        event.setStatus(status);
        event.setSyncedAt(Instant.now());
        repository.save(event);
    }
}
