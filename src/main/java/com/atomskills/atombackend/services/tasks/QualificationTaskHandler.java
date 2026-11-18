package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.atomskills.atombackend.dto.task.QualificationBatchTask;
import com.atomskills.atombackend.dto.task.QualificationTask;
import com.atomskills.atombackend.entity.Employee;
import com.atomskills.atombackend.entity.Qualification;
import com.atomskills.atombackend.repository.QualificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class QualificationTaskHandler {
    private final QualificationRepository repository;
    private final EmployeeTaskHandler employees;

    @Transactional
    @ArgentQueueHandler("external-qualification-batch")
    public void handleBatch(QualificationBatchTask batch) {
        for (var task : batch.getItems()) {
            handle(task);
        }
    }

    @Transactional
    @ArgentQueueHandler("external-qualification")
    public void handle(QualificationTask task) {
        Employee employee = employees.ensurePresent(task.getIdEmployee());

        Qualification entity = repository.findById(task.getIdQualification())
                .orElseGet(Qualification::new);
        entity.setIdQualification(task.getIdQualification());
        entity.setEmployee(employee);
        entity.setResourceType(task.getResourceType());
        entity.setProff(task.getProff());
        entity.setSyncedAt(Instant.now());
        repository.save(entity);
    }
}
