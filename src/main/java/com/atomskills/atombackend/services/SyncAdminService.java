package com.atomskills.atombackend.services;

import com.atomskills.atombackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbc;

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

        // Один UPDATE на все источники — сбрасываем все курсоры разом.
        jdbc.update("UPDATE external_sync_cursor SET last_seen_id = 0, updated_at = now()");
    }
}
