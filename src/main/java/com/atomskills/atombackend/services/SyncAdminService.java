package com.atomskills.atombackend.services;

import com.argent.servicecommon.queue.service.ArgentQueueService;
import com.atomskills.atombackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncAdminService {

    private final QualificationRepository qualifications;
    private final TimeResolveRepository timeResolves;
    private final EmployeeRepository employees;
    private final EdificeDependencyRepository dependencies;
    private final EventRepository events;
    private final EventStatusRepository eventStatuses;
    private final EdificeRepository edifices;
    private final ExternalCursorRepository cursorRepository;
    private final ArgentQueueService queueService;

    /**
     * Полная очистка всех синхронизированных таблиц и сброс всех курсоров.
     * Порядок удаления — от детей к родителям, чтобы не нарушать FK.
     */
    @Transactional
    public void clearAll() {
        qualifications.deleteAllInBatch();
        timeResolves.deleteAllInBatch();
        employees.deleteAllInBatch();
        dependencies.deleteAllInBatch();
        events.deleteAllInBatch();
        eventStatuses.deleteAllInBatch();
        edifices.deleteAllInBatch();
        cursorRepository.resetAllCursors();

    }
}
