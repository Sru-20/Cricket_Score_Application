package com.boundary.api;

import com.boundary.api.MatchDtos.Commentary;
import com.boundary.api.MatchDtos.MatchView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreServiceTest {
    private final FakeSnapshots snapshots = new FakeSnapshots();
    private ScoreService scores;

    @BeforeEach
    void setUp() {
        scores = new ScoreService((destination, payload) -> {}, snapshots);
        scores.loadMatches();
    }

    @Test
    void recordsDeliveryAndPersistsSnapshotAndBallResult() {
        MatchView result = scores.recordBall("ind-aus-2026", 4, false);

        assertEquals(291, result.homeTeam().runs());
        assertEquals("4", result.recentBalls().getLast());
        assertEquals(result, snapshots.matches.get(result.id()));
        assertEquals(new SavedBall(result.id(), "43.3", 4, false), snapshots.balls.getLast());
    }

    @Test
    void restoresThePersistedScoreAfterRestart() {
        scores.recordBall("ind-aus-2026", 6, false);

        ScoreService restarted = new ScoreService((destination, payload) -> {}, snapshots);
        restarted.loadMatches();

        MatchView restored = restarted.getMatch("ind-aus-2026");
        assertEquals(293, restored.homeTeam().runs());
        assertEquals("6", restored.recentBalls().getLast());
        assertEquals(54, restarted.getScorecard("ind-aus-2026").bowlers().getFirst().runs());
    }

    @Test
    void rejectsFiveRunsWithoutPersistingDelivery() {
        int currentBallCount = snapshots.balls.size();
        assertThrows(ResponseStatusException.class, () -> scores.recordBall("ind-aus-2026", 5, false));
        assertEquals(currentBallCount, snapshots.balls.size());
    }

    private static final class FakeSnapshots extends MatchSnapshotRepository {
        private final Map<String, MatchView> matches = new HashMap<>();
        private final List<SavedBall> balls = new ArrayList<>();

        private FakeSnapshots() {
            super(new JdbcTemplate(new DriverManagerDataSource()), new ObjectMapper());
        }

        @Override
        public List<MatchView> findAll() {
            return List.copyOf(matches.values());
        }

        @Override
        public void save(MatchView match) {
            matches.put(match.id(), match);
        }

        @Override
        public void saveDelivery(MatchView match, int runs, boolean wicket, Commentary commentary) {
            save(match);
            balls.add(new SavedBall(match.id(), commentary.over(), runs, wicket));
        }
    }

    private record SavedBall(String matchId, String over, int runs, boolean wicket) {}
}
