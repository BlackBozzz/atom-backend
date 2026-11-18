package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.task.TimeResolveBatchTask;
import com.atomskills.atombackend.dto.task.TimeResolveTask;
import com.atomskills.atombackend.entity.TimeResolve;
import com.atomskills.atombackend.repository.TimeResolveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TimeResolveTaskHandler {
    private final TimeResolveRepository repository;
    private final EdificeTaskHandler edifices;

    @Transactional
    @ArgentQueueHandler("external-time-resolve-batch")
    public void handleBatch(TimeResolveBatchTask batch) {
        for (var task : batch.getItems()) {
            handle(task);
        }
    }

    @Transactional
    @ArgentQueueHandler("external-time-resolve")
    public void handle(TimeResolveTask task) {
        var edifice = edifices.ensurePresent(task.getIdEdifice());

        TimeResolve entity = repository.findById(task.getIdTime())
                .orElseGet(TimeResolve::new);
        entity.setIdTime(task.getIdTime());
        entity.setResourceType(task.getResourceType());
        entity.setAvgResolutionTime(task.getAvgResolutionTime());
        entity.setEdifice(edifice);
        entity.setSyncedAt(Instant.now());
        repository.save(entity);
    }
}