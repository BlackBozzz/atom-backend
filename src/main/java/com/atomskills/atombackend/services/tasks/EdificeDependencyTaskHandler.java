package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.task.EdificeDependencyBatchTask;
import com.atomskills.atombackend.dto.task.EdificeDependencyTask;
import com.atomskills.atombackend.entity.EdificeDependency;
import com.atomskills.atombackend.repository.EdificeDependencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EdificeDependencyTaskHandler {
    private final EdificeDependencyRepository repository;
    private final EdificeTaskHandler edifices;

    @Transactional
    @ArgentQueueHandler("external-edifice-dependency-batch")
    public void handleBatch(EdificeDependencyBatchTask batch) {
        for (var task : batch.getItems()) {
            handle(task);
        }
    }

    @Transactional
    @ArgentQueueHandler("external-edifice-dependency")
    public void handle(EdificeDependencyTask task) {
        var source = edifices.ensurePresent(task.getIdEdificeSource());
        var dependent = edifices.ensurePresent(task.getIdEdificeDependent());
        EdificeDependency dependency = repository.findById(task.getIdDependence())
                .orElseGet(EdificeDependency::new);
        dependency.setIdDependence(task.getIdDependence());
        dependency.setResourceType(task.getResourceType());
        dependency.setSource(source);
        dependency.setDependent(dependent);
        dependency.setSyncedAt(Instant.now());
        repository.save(dependency);
    }
}
