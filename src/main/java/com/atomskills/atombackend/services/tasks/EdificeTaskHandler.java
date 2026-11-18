package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.task.EdificeBatchTask;
import com.atomskills.atombackend.dto.task.EdificeTask;
import com.atomskills.atombackend.entity.Edifice;
import com.atomskills.atombackend.reader.EdificeReader;
import com.atomskills.atombackend.repository.EdificeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EdificeTaskHandler {
    private final EdificeRepository repository;
    private final EdificeReader reader;

    @Transactional
    @ArgentQueueHandler("external-edifice-batch")
    public void handleBatch(EdificeBatchTask batch) {
        for (var task : batch.getItems()) {
            handle(task);
        }
    }

    @Transactional
    @ArgentQueueHandler("external-edifice")
    public void handle(EdificeTask task) {
        Edifice entity = repository.findById(task.getIdEdifice()).orElseGet(Edifice::new);
        entity.setIdEdifice(task.getIdEdifice());
        entity.setName(task.getName());
        entity.setResourceType(task.getResourceType());
        entity.setDistrict(task.getDistrict());
        entity.setStreet(task.getStreet());
        entity.setHouseNumber(task.getHouseNumber());
        entity.setSyncedAt(Instant.now());
        repository.save(entity);
    }

    /** Подтягивает поздно появившегося родителя непосредственно перед ребёнком. */
    public Edifice ensurePresent(Long id) {
        return repository.findById(id).orElseGet(() -> {
            EdificeTask task = reader.findById(id).orElseThrow(() -> new IllegalStateException(
                    "Во внешней БД отсутствует edifice " + id + ", на него ссылается зависимая строка"));
            handle(task);
            return repository.findById(id).orElseThrow();
        });
    }
}
