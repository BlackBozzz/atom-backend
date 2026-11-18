package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.task.EventTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventReader {

    private static final String SQL = """
            SELECT id_event, id_edifice, resource_type, occurred_at, id_status
              FROM events
             WHERE id_event > ?
             ORDER BY id_event
             LIMIT ?
            """;

    private final JdbcTemplate legacyJdbc;

    public EventReader(@Qualifier("externalJdbcTemplate") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<EventTask> fetchSince(long lastSeenId, int limit) {
        return legacyJdbc.query(SQL, (rs, n) -> new EventTask(
                rs.getLong("id_event"),
                rs.getLong("id_edifice"),
                rs.getString("resource_type"),
                rs.getTimestamp("occurred_at").toLocalDateTime(),
                rs.getLong("id_status")), lastSeenId, limit);
    }
}