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
        Edifice source = edifices.findById(t.idEdificeSource())
                .orElseThrow(() -> new IllegalStateException(
                        "edifice source " + t.idEdificeSource() + " ещё не синхронизирован"));
        Edifice dependent = edifices.findById(t.idEdificeDependent())
                .orElseThrow(() -> new IllegalStateException(
                        "edifice dependent " + t.idEdificeDependent() + " ещё не синхронизирован"));

        EdificeDependency d = repository.findById(t.idDependence()).orElseGet(EdificeDependency::new);
        d.setIdDependence(t.idDependence());
        d.setResourceType(t.resourceType());
        d.setSource(source);
        d.setDependent(dependent);
        d.setSyncedAt(Instant.now());
        repository.save(d);
    }
}
