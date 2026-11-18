package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.DependencyStatusDto;
import com.atomskills.atombackend.dto.EdificeStateDto;
import com.atomskills.atombackend.dto.StatusDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Чтение из основной (локальной) БД; не из externalJdbcTemplate. */
@Component
public class EdificeStatusReader {

    private static final String SQL_EDIFICES = """
            SELECT id_edifice, name, resource_type, district, street, house_number
              FROM edifice
             ORDER BY id_edifice
            """;

    private static final String SQL_DEPENDENCIES = """
            SELECT id_dependence, resource_type, id_edifice_source, id_edifice_dependent
              FROM edifice_dependency
             ORDER BY id_dependence
            """;

    private static final String SQL_STATUS_START = """
            SELECT ed.id_edifice,
                   r.resource_type,
                   last_event.id_status,
                   st.name AS status_name,
                   last_event.occurred_at
              FROM edifice ed
              CROSS JOIN LATERAL unnest(ed.resource_type) AS r(resource_type)
              LEFT JOIN LATERAL (
                  SELECT ev.id_status, ev.occurred_at
                    FROM event ev
                   WHERE ev.id_edifice = ed.id_edifice
                     AND ev.resource_type = r.resource_type
            """;

    private static final String SQL_STATUS_END = """
                   ORDER BY ev.occurred_at DESC, ev.id_event DESC
                   LIMIT 1
              ) last_event ON TRUE
              LEFT JOIN event_status st ON st.id_status = last_event.id_status
             ORDER BY ed.id_edifice, r.resource_type
            """;

    private static final String SQL_CURRENT_STATUSES = SQL_STATUS_START + SQL_STATUS_END;
    // Событие ровно в момент at включается в выборку.
    private static final String SQL_HISTORICAL_STATUSES =
            SQL_STATUS_START + " AND ev.occurred_at <= ?\n" + SQL_STATUS_END;

    private final JdbcTemplate jdbcTemplate;

    public EdificeStatusReader(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EdificeStateDto> findEdifices() {
        return jdbcTemplate.query(SQL_EDIFICES, (rs, rowNum) -> {
            Array sqlArray = rs.getArray("resource_type");
            List<String> types = new ArrayList<>();
            if (sqlArray != null) {
                try {
                    types.addAll(Arrays.asList((String[]) sqlArray.getArray()));
                } finally {
                    sqlArray.free();
                }
            }
            return new EdificeStateDto(
                    rs.getInt("id_edifice"),
                    rs.getString("name"),
                    types,
                    rs.getString("district"),
                    rs.getString("street"),
                    rs.getString("house_number"),
                    new ArrayList<>(),
                    new ArrayList<>());
        });
    }

    /** Карта: id здания -> (тип ресурса -> его последний статус, либо null). */
    public Map<Integer, Map<String, StatusDto>> findResourceStatuses(LocalDateTime at) {
        ResultSetExtractor<Map<Integer, Map<String, StatusDto>>> extractor = rs -> {
            Map<Integer, Map<String, StatusDto>> result = new HashMap<>();
            while (rs.next()) {
                int edificeId = rs.getInt("id_edifice");
                String resourceType = rs.getString("resource_type");
                Number idStatus = (Number) rs.getObject("id_status");
                Timestamp occurredAt = rs.getTimestamp("occurred_at");
                StatusDto status = idStatus == null ? null : new StatusDto(
                        idStatus.shortValue(),
                        rs.getString("status_name"),
                        occurredAt == null ? null : occurredAt.toLocalDateTime());
                result.computeIfAbsent(edificeId, ignored -> new HashMap<>())
                        .put(resourceType, status);
            }
            return result;
        };
        return at == null
                ? jdbcTemplate.query(SQL_CURRENT_STATUSES, extractor)
                : jdbcTemplate.query(SQL_HISTORICAL_STATUSES, extractor, Timestamp.valueOf(at));
    }

    public List<DependencyStatusDto> findDependencies() {
        return jdbcTemplate.query(SQL_DEPENDENCIES, (rs, rowNum) ->
                new DependencyStatusDto(
                        rs.getInt("id_dependence"),
                        rs.getString("resource_type"),
                        rs.getInt("id_edifice_source"),
                        rs.getInt("id_edifice_dependent"),
                        null));
    }
}
