package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.task.QualificationTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QualificationReader {
    private static final String SQL = """
            SELECT id_qualification, id_employee, resource_type, proff
              FROM qualification
             WHERE id_qualification > ?
             ORDER BY id_qualification
             LIMIT ?
            """;

    private final JdbcTemplate legacyJdbc;

    public QualificationReader(@Qualifier("externalJdbcTemplate") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<QualificationTask> fetchSince(long lastSeenId, int limit) {
        return legacyJdbc.query(SQL, (rs, n) -> new QualificationTask(
                rs.getLong("id_qualification"),
                rs.getLong("id_employee"),
                rs.getString("resource_type"),
                rs.getBigDecimal("proff")), lastSeenId, limit);
    }
}