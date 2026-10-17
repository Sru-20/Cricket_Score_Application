package com.boundary.api;

import com.boundary.api.MatchDtos.MatchView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class MatchSnapshotRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public MatchSnapshotRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<MatchView> findAll() {
        return jdbc.query("SELECT state::text FROM match_snapshots ORDER BY id",
                (result, rowNumber) -> {
                    try {
                        return objectMapper.readValue(result.getString(1), MatchView.class);
                    } catch (JsonProcessingException exception) {
                        throw new IllegalStateException("Unable to deserialize a persisted match snapshot", exception);
                    }
                });
    }

    public void save(MatchView match) {
        saveSnapshot(match);
    }

    @Transactional
    public void saveDelivery(MatchView match, int runs, boolean wicket, MatchDtos.Commentary commentary) {
        saveSnapshot(match);
        jdbc.update("""
                INSERT INTO ball_results (match_id, over_label, runs, wicket, commentary)
                VALUES (?, ?, ?, ?, ?)
                """, match.id(), commentary.over(), runs, wicket, commentary.text());
    }

    private void saveSnapshot(MatchView match) {
        try {
            String json = objectMapper.writeValueAsString(match);
            jdbc.update("""
                    INSERT INTO match_snapshots (id, state, updated_at)
                    VALUES (?, CAST(? AS jsonb), CURRENT_TIMESTAMP)
                    ON CONFLICT (id) DO UPDATE
                    SET state = EXCLUDED.state, updated_at = EXCLUDED.updated_at
                    """, match.id(), json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize match snapshot " + match.id(), exception);
        }
    }
}
