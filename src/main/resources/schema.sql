CREATE TABLE IF NOT EXISTS worlds (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(24) NOT NULL,
    state_json TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS world_events (
    event_id VARCHAR(36) PRIMARY KEY,
    world_id VARCHAR(36) NOT NULL,
    action_id VARCHAR(36),
    sequence_number BIGINT NOT NULL,
    event_index INTEGER NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    event_json TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_world_events_order ON world_events(world_id, sequence_number, event_index);

CREATE TABLE IF NOT EXISTS world_snapshots (
    world_id VARCHAR(36) NOT NULL,
    sequence_number BIGINT NOT NULL,
    state_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY(world_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS processed_action_events (
    action_id VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
