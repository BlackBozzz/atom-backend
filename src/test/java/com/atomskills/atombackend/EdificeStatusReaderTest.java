package com.atomskills.atombackend;

import com.atomskills.atombackend.dto.EdificeStateDto;
import com.atomskills.atombackend.dto.StatusDto;
import com.atomskills.atombackend.reader.EdificeStatusReader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EdificeStatusReaderTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void currentSqlSelectsLastEventForEachResourceIncludingMissingEvents() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        EdificeStatusReader reader = new EdificeStatusReader(jdbc);
        reader.findResourceStatuses(null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ResultSetExtractor> extractor = ArgumentCaptor.forClass(ResultSetExtractor.class);
        verify(jdbc).query(sql.capture(), extractor.capture());
        assertTrue(sql.getValue().contains("CROSS JOIN LATERAL unnest(ed.resource_type)"));
        assertTrue(sql.getValue().contains("ev.resource_type = r.resource_type"));
        assertTrue(sql.getValue().contains("LEFT JOIN LATERAL"));
        assertTrue(sql.getValue().contains("ORDER BY ev.occurred_at DESC, ev.id_event DESC"));
        assertFalse(sql.getValue().contains("ev.occurred_at <= ?"));

        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getInt("id_edifice")).thenReturn(7, 7);
        when(rs.getString("resource_type")).thenReturn("electricity", "water");
        when(rs.getObject("id_status")).thenReturn((short) 1, null);
        when(rs.getString("status_name")).thenReturn("Норма");
        LocalDateTime t = LocalDateTime.of(2026, 11, 18, 10, 30);
        when(rs.getTimestamp("occurred_at")).thenReturn(Timestamp.valueOf(t), null);

        Map<Integer, Map<String, StatusDto>> statuses = (Map<Integer, Map<String, StatusDto>>)
                extractor.getValue().extractData(rs);
        assertEquals((short) 1, statuses.get(7).get("electricity").getIdStatus());
        assertEquals(t, statuses.get(7).get("electricity").getOccurredAt());
        assertTrue(statuses.get(7).containsKey("water"));
        assertNull(statuses.get(7).get("water"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void historicalSqlIncludesTimestampAtBoundaryAndDoesNotFilterWholeBuildings() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        EdificeStatusReader reader = new EdificeStatusReader(jdbc);
        LocalDateTime at = LocalDateTime.of(2026, 11, 18, 11, 15);
        reader.findResourceStatuses(at);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ResultSetExtractor> extractor = ArgumentCaptor.forClass(ResultSetExtractor.class);
        verify(jdbc).query(sql.capture(), extractor.capture(), eq(Timestamp.valueOf(at)));
        assertTrue(sql.getValue().contains("AND ev.occurred_at <= ?"));
        assertTrue(sql.getValue().contains("LEFT JOIN LATERAL"));
        assertTrue(sql.getValue().contains("LEFT JOIN event_status"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void findsEdificesAndReadsPostgresArray() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        EdificeStatusReader reader = new EdificeStatusReader(jdbc);
        reader.findEdifices();

        ArgumentCaptor<RowMapper> mapper = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbc).query(anyString(), mapper.capture());
        ResultSet rs = mock(ResultSet.class);
        Array types = mock(Array.class);
        when(rs.getInt("id_edifice")).thenReturn(8);
        when(rs.getString("name")).thenReturn("Насосная станция №1");
        when(rs.getArray("resource_type")).thenReturn(types);
        when(types.getArray()).thenReturn(new String[]{"water", "electricity"});

        EdificeStateDto dto = (EdificeStateDto) mapper.getValue().mapRow(rs, 0);
        assertEquals(8, dto.getIdEdifice());
        assertEquals(2, dto.getResourceTypes().size());
        assertTrue(dto.getResources().isEmpty());
        assertTrue(dto.getDependencies().isEmpty());
        verify(types).free();
    }
}
