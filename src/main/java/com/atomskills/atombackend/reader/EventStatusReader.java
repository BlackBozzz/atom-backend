package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.EventStatusTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventStatusReader {

    private static final String SQL = """
            SELECT id_status, name
              FROM event_statuses
             WHERE id_status > ?
             ORDER BY id_status
             LIMIT ?
            """;

    private final JdbcTemplate legacyJdbc;

    public EventStatusReader(@Qualifier("legacyJdbc") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<EventStatusTask> fetchSince(long lastSeenId, int limit) {
        return legacyJdbc.query(SQL,
                (rs, n) -> new EventStatusTask(rs.getShort("id_status"), rs.getString("name")),
                lastSeenId, limit);
    }
}