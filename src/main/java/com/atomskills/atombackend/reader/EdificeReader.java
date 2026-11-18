package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.task.EdificeTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Component
public class EdificeReader {
    private static final String COLUMNS = "id_edifice, name, resource_type, district, street, house_number";
    private static final RowMapper<EdificeTask> MAPPER = (rs, row) -> {
        try {
            Array array = rs.getArray("resource_type");
            String[] types = array == null ? new String[0] : (String[]) array.getArray();
            return new EdificeTask(
                    rs.getLong("id_edifice"), rs.getString("name"), types,
                    rs.getString("district"), rs.getString("street"), rs.getString("house_number"));
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать resource_type", e);
        }
    };
    private final JdbcTemplate legacyJdbc;

    public EdificeReader(@Qualifier("externalJdbcTemplate") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<EdificeTask> fetchSince(long lastSeenId, int limit) {
        return legacyJdbc.query("SELECT " + COLUMNS + " FROM edifices " +
                "WHERE id_edifice > ? ORDER BY id_edifice LIMIT ?", MAPPER, lastSeenId, limit);
    }

    public Optional<EdificeTask> findById(Long id) {
        List<EdificeTask> rows = legacyJdbc.query(
                "SELECT " + COLUMNS + " FROM edifices WHERE id_edifice = ?", MAPPER, id);
        return rows.stream().findFirst();
    }
}
