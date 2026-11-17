package com.atomskills.atombackend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExternalCursorRepository {

    private final JdbcTemplate jdbc;

    public ExternalCursorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long getLastSeenId(String source) {
        Long v = jdbc.queryForObject(
                "SELECT last_seen_id FROM external_sync_cursor WHERE source = ?",
                Long.class, source);
        return v == null ? 0L : v;
    }

    /** Только вперёд — защита от отката при гонках. */
    public void advance(String source, long newLastSeenId) {
        jdbc.update("""
                UPDATE external_sync_cursor
                   SET last_seen_id = ?, updated_at = now()
                 WHERE source = ?
                   AND last_seen_id < ?
                """, newLastSeenId, source, newLastSeenId);
    }
}