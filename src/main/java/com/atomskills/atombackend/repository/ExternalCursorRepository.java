package com.atomskills.atombackend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExternalCursorRepository {
    private final JdbcTemplate jdbc;

    public ExternalCursorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long getLastSeenId(String source) {
        Long value = jdbc.queryForObject(
                "SELECT last_seen_id FROM external_sync_cursor WHERE source = ?", Long.class, source);
        return value == null ? 0L : value;
    }

    public void advance(String source, long newLastSeenId) {
        int updated = jdbc.update("""
                UPDATE external_sync_cursor
                   SET last_seen_id = ?, updated_at = now()
                 WHERE source = ? AND last_seen_id < ?
                """, newLastSeenId, source, newLastSeenId);
        if (updated != 1) {
            throw new IllegalStateException("Не удалось передвинуть курсор " + source + " до " + newLastSeenId);
        }
    }

    /**
     * Переход со старых независимых очередей на одну строгую FIFO.
     * Метод должен вызываться в транзакции приложения под advisory lock.
     * Старые задания не удаляются, а перестают исполняться; все строки переотправляются.
     */
    public boolean prepareOrderedQueue() {
        jdbc.update("""
                INSERT INTO external_sync_cursor (source, last_seen_id)
                VALUES ('legacy-sync-ordered-v1', 0)
                ON CONFLICT (source) DO NOTHING
                """);
        jdbc.update("""
                UPDATE argent_queue SET paused = true, updated_at = now()
                WHERE name IN ('external-sync-edifices', 'external-sync-event_statuses',
                               'external-sync-events', 'external-sync-edifice_dependencies',
                               'external-time-resolve','"external-qualification', 'external-employee')
                  AND NOT paused
                """);
        Long active = jdbc.queryForObject("""
                SELECT count(*) FROM argent_queue_task t
                JOIN argent_queue q ON q.id = t.queue_id
                WHERE q.name IN ('legacy-sync-edifices', 'legacy-sync-event_statuses',
                                 'legacy-sync-events', 'legacy-sync-edifice_dependencies',
                                 'external-time-resolve','"external-qualification', 'external-employee')
                  AND t.status = 'RUNNING'
                """, Long.class);
        if (active != null && active > 0) return false;
        if (getLastSeenId("legacy-sync-ordered-v1") == 1) return true;

        jdbc.update("""
                UPDATE external_sync_cursor SET last_seen_id = 0, updated_at = now()
                WHERE source IN ('edifices', 'event_statuses', 'events', 'edifice_dependencies', 'employees', 'qualifications', 'time_resolve')
                """);
        jdbc.update("""
                UPDATE external_sync_cursor SET last_seen_id = 1, updated_at = now()
                WHERE source = 'legacy-sync-ordered-v1'
                """);
        return true;
    }

    public void resetAllCursors() {
        jdbc.update("UPDATE external_sync_cursor SET last_seen_id = 0, updated_at = now()");
        jdbc.update("TRUNCATE TABLE argent_queue CONTINUE IDENTITY CASCADE");
    }
}
