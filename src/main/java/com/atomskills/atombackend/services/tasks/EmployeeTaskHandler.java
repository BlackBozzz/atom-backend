package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.task.EmployeeBatchTask;
import com.atomskills.atombackend.dto.task.EmployeeTask;
import com.atomskills.atombackend.entity.Employee;
import com.atomskills.atombackend.reader.EmployeeReader;
import com.atomskills.atombackend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EmployeeTaskHandler {
    private final EmployeeRepository repository;
    private final EmployeeReader reader;

    @Transactional
    @ArgentQueueHandler("external-employee-batch")
    public void handleBatch(EmployeeBatchTask batch) {
        for (var task : batch.getItems()) {
            handle(task);
        }
    }

    @Transactional
    @ArgentQueueHandler("external-employee")
    public void handle(EmployeeTask task) {
        Employee entity = repository.findById(task.getTabel()).orElseGet(Employee::new);
        entity.setTabel(task.getTabel());
        entity.setFirstName(task.getFirstName());
        entity.setLastName(task.getLastName());
        entity.setMiddleName(task.getMiddleName());
        entity.setStartDateExperience(task.getStartDateExperience());
        entity.setSyncedAt(Instant.now());
        repository.save(entity);
    }

    /** Подтягивает поздно появившегося родителя непосредственно перед ребёнком. */
    public Employee ensurePresent(long tabel) {
        return repository.findById(tabel).orElseGet(() -> {
            EmployeeTask task = reader.findById(tabel).orElseThrow(() -> new IllegalStateException(
                    "Во внешней БД отсутствует employee " + tabel + ", на него ссылается квалификация"));
            handle(task);
            return repository.findById(tabel).orElseThrow();
        });
    }
}