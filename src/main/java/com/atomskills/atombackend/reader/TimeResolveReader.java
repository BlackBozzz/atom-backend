package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.task.TimeResolveTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class TimeResolveReader {
    private static final String SQL = """
            SELECT id_time,
                   resource_type,
                   id_edifice,
                   -- PostgreSQL сам конвертирует interval в ISO-8601 строку
                   CASE
                       WHEN avg_resolution_time IS NULL THEN NULL
                       ELSE 'PT' || EXTRACT(EPOCH FROM avg_resolution_time)::text || 'S'
                   END AS avg_resolution_time_iso
              FROM time_resolve
             WHERE id_time > ?
             ORDER BY id_time
             LIMIT ?
            """;

    private final JdbcTemplate legacyJdbc;

    public TimeResolveReader(@Qualifier("externalJdbcTemplate") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<TimeResolveTask> fetchSince(long lastSeenId, int limit) {
        return legacyJdbc.query(SQL, (rs, n) -> new TimeResolveTask(
                rs.getInt("id_time"),
                rs.getString("resource_type"),
                rs.getString("avg_resolution_time_iso"),
                rs.getInt("id_edifice")
        ), lastSeenId, limit);
    }
}