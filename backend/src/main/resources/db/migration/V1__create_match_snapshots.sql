CREATE TABLE match_snapshots (
    id VARCHAR(100) PRIMARY KEY,
    state JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_match_snapshots_updated_at ON match_snapshots (updated_at DESC);

CREATE TABLE ball_results (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    match_id VARCHAR(100) NOT NULL REFERENCES match_snapshots (id) ON DELETE CASCADE,
    over_label VARCHAR(12) NOT NULL,
    runs SMALLINT NOT NULL CHECK (runs BETWEEN 0 AND 6),
    wicket BOOLEAN NOT NULL,
    commentary TEXT NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ball_results_match_recorded ON ball_results (match_id, recorded_at DESC);
