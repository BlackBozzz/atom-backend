package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.task.EventStatusBatchTask;
import com.atomskills.atombackend.dto.task.EventStatusTask;
import com.atomskills.atombackend.entity.EventStatus;
import com.atomskills.atombackend.reader.EventStatusReader;
import com.atomskills.atombackend.repository.EventStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EventStatusTaskHandler {
    private final EventStatusRepository repository;
    private final EventStatusReader reader;

    @Transactional
    @ArgentQueueHandler("external-event-status-batch")
    public void handleBatch(EventStatusBatchTask batch) {
        for (var task : batch.getItems()) {
            handle(task);
        }
    }

    @Transactional
    @ArgentQueueHandler("external-event-status")
    public void handle(EventStatusTask task) {
        EventStatus entity = repository.findById(task.getIdStatus()).orElseGet(EventStatus::new);
        entity.setIdStatus(task.getIdStatus());
        entity.setName(task.getName());
        entity.setSyncedAt(Instant.now());
        repository.save(entity);
    }

    public EventStatus ensurePresent(Long id) {
        return repository.findById(id).orElseGet(() -> {
            EventStatusTask task = reader.findById(id).orElseThrow(() -> new IllegalStateException(
                    "Во внешней БД отсутствует event_status " + id + ", на него ссылается событие"));
            handle(task);
            return repository.findById(id).orElseThrow();
        });
    }
}
