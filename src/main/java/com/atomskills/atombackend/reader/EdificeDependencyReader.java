package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.task.EdificeDependencyTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EdificeDependencyReader {

    private static final String SQL = """
            SELECT id_dependence, resource_type, id_edifice_source, id_edifice_dependent
              FROM edifice_dependencies
             WHERE id_dependence > ?
             ORDER BY id_dependence
             LIMIT ?
            """;

    private final JdbcTemplate legacyJdbc;

    public EdificeDependencyReader(@Qualifier("externalJdbcTemplate") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<EdificeDependencyTask> fetchSince(long lastSeenId, int limit) {
        return legacyJdbc.query(SQL, (rs, n) -> new EdificeDependencyTask(
                rs.getInt("id_dependence"),
                rs.getString("resource_type"),
                rs.getInt("id_edifice_source"),
                rs.getInt("id_edifice_dependent")), lastSeenId, limit);
    }
}