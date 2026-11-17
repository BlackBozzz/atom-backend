package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.EdificeTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.SQLException;
import java.util.List;

@Component
public class EdificeReader {

    private static final String SQL = """
            SELECT id_edifice, name, resource_type, district, street, house_number
              FROM edifices
             WHERE id_edifice > ?
             ORDER BY id_edifice
             LIMIT ?
            """;

    private static final RowMapper<EdificeTask> MAPPER = (rs, n) -> {
        try {
            Array arr = rs.getArray("resource_type");
            String[] types = arr == null ? new String[0] : (String[]) arr.getArray();
            return new EdificeTask(
                    rs.getInt("id_edifice"),
                    rs.getString("name"),
                    types,
                    rs.getString("district"),
                    rs.getString("street"),
                    rs.getString("house_number"));
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать resource_type", e);
        }
    };

    private final JdbcTemplate legacyJdbc;

    public EdificeReader(@Qualifier("externalJdbcTemplate") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<EdificeTask> fetchSince(long lastSeenId, int limit) {
        return legacyJdbc.query(SQL, MAPPER, lastSeenId, limit);
    }
}