package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.EdificeDependencyTask;
import com.atomskills.atombackend.entity.Edifice;
import com.atomskills.atombackend.entity.EdificeDependency;
import com.atomskills.atombackend.repository.EdificeDependencyRepository;
import com.atomskills.atombackend.repository.EdificeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EdificeDependencyTaskHandler {
    private final EdificeDependencyRepository repository;
    private final EdificeRepository edifices;

    @ArgentQueueHandler("external-edifice-dependency")
    public void handle(EdificeDependencyTask t) {
        Edifice source = edifices.findById(t.getIdEdificeSource())
                .orElseThrow(() -> new IllegalStateException(
                        "edifice source " + t.getIdEdificeSource() + " ещё не синхронизирован"));
        Edifice dependent = edifices.findById(t.getIdEdificeDependent())
                .orElseThrow(() -> new IllegalStateException(
                        "edifice dependent " + t.getIdEdificeDependent() + " ещё не синхронизирован"));

        EdificeDependency d = repository.findById(t.getIdDependence()).orElseGet(EdificeDependency::new);
        d.setIdDependence(t.getIdDependence());
        d.setResourceType(t.getResourceType());
        d.setSource(source);
        d.setDependent(dependent);
        d.setSyncedAt(Instant.now());
        repository.save(d);
    }
}
