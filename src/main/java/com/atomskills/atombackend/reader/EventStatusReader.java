package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.task.EventStatusTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EventStatusReader {
    private final JdbcTemplate legacyJdbc;

    public EventStatusReader(@Qualifier("externalJdbcTemplate") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<EventStatusTask> fetchSince(long lastSeenId, int limit) {
        return legacyJdbc.query("""
                SELECT id_status, name FROM event_statuses
                WHERE id_status > ? ORDER BY id_status LIMIT ?
                """, (rs, row) -> new EventStatusTask(
                        rs.getLong("id_status"), rs.getString("name")
                ),
                lastSeenId, limit);
    }

    public Optional<EventStatusTask> findById(Long id) {
        List<EventStatusTask> rows = legacyJdbc.query("""
                SELECT id_status, name FROM event_statuses WHERE id_status = ?
                """, (rs, row) -> new EventStatusTask(
                        rs.getLong("id_status"), rs.getString("name")
        ), id);
        return rows.stream().findFirst();
    }
}
