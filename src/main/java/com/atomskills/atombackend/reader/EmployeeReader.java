package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.task.EmployeeTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Component
public class EmployeeReader {
    private static final String COLUMNS =
            "tabel, first_name, last_name, middle_name, start_date_experience";

    private static final RowMapper<EmployeeTask> MAPPER = (rs, row) -> {
        Date expDate = rs.getDate("start_date_experience");
        return new EmployeeTask(
                rs.getLong("tabel"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("middle_name"),
                expDate == null ? null : expDate.toLocalDate());
    };

    private final JdbcTemplate legacyJdbc;

    public EmployeeReader(@Qualifier("externalJdbcTemplate") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<EmployeeTask> fetchSince(long lastSeenId, int limit) {
        return legacyJdbc.query(
                "SELECT " + COLUMNS + " FROM employee " +
                        "WHERE tabel > ? ORDER BY tabel LIMIT ?",
                MAPPER, lastSeenId, limit);
    }

    public Optional<EmployeeTask> findById(long tabel) {
        List<EmployeeTask> rows = legacyJdbc.query(
                "SELECT " + COLUMNS + " FROM employee WHERE tabel = ?", MAPPER, tabel);
        return rows.stream().findFirst();
    }
}