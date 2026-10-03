package org.infospray.peonsimulator.adapter.secondary.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcEventRepository implements EventRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcEventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void appendAll(List<SimulationEvent> events) {
        for (SimulationEvent event : events) {
            this.jdbcTemplate.update("INSERT INTO world_events(event_id, world_id, action_id, sequence_number, event_index, event_type, event_json, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", event.eventId().toString(), event.worldId().toString(), event.actionId() == null ? null : event.actionId().toString(), event.sequenceNumber(), event.eventIndex(), event.eventType(), this.write(event), Timestamp.from(event.occurredAt()));
        }
    }

    @Override
    public List<SimulationEvent> findByWorldId(UUID worldId) {
        return this.jdbcTemplate.query("SELECT event_json FROM world_events WHERE world_id = ? ORDER BY sequence_number, event_index", (resultSet, rowNumber) -> this.read(resultSet.getString(1)), worldId.toString());
    }

    @Override
    public boolean actionWasProcessed(UUID actionId) {
        Integer count = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM processed_action_events WHERE action_id = ?", Integer.class, actionId.toString());
        return count != null && count > 0;
    }

    @Override
    public void markActionProcessed(UUID actionId) {
        try { this.jdbcTemplate.update("INSERT INTO processed_action_events(action_id) VALUES (?)", actionId.toString()); } catch (DuplicateKeyException ignored) { }
    }

    @Override
    public void deleteAll() {
        this.jdbcTemplate.update("DELETE FROM world_events");
        this.jdbcTemplate.update("DELETE FROM processed_action_events");
    }

    private String write(SimulationEvent event) {
        try { return this.objectMapper.writeValueAsString(event); } catch (JsonProcessingException exception) { throw new IllegalStateException("Impossible de sérialiser l'événement", exception); }
    }

    private SimulationEvent read(String json) {
        try { return this.objectMapper.readValue(json, SimulationEvent.class); } catch (JsonProcessingException exception) { throw new IllegalStateException("Impossible de désérialiser l'événement", exception); }
    }
}
