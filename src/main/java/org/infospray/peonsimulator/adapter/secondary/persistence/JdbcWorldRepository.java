package org.infospray.peonsimulator.adapter.secondary.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcWorldRepository implements WorldRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcWorldRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public World save(World world) {
        String json = this.write(world);
        int updated = this.jdbcTemplate.update("UPDATE worlds SET status = ?, state_json = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", world.getStatus().name(), json, world.getId().toString());
        if (updated == 0) {
            this.jdbcTemplate.update("INSERT INTO worlds(id, status, state_json) VALUES (?, ?, ?)", world.getId().toString(), world.getStatus().name(), json);
        }
        return world;
    }

    @Override
    public void saveSnapshot(World world) {
        String json = this.write(world);
        int updated = this.jdbcTemplate.update("UPDATE world_snapshots SET state_json = ?, created_at = CURRENT_TIMESTAMP WHERE world_id = ? AND sequence_number = ?", json, world.getId().toString(), world.getSequenceNumber());
        if (updated == 0) { this.jdbcTemplate.update("INSERT INTO world_snapshots(world_id, sequence_number, state_json) VALUES (?, ?, ?)", world.getId().toString(), world.getSequenceNumber(), json); }
    }

    @Override
    public Optional<World> findById(UUID worldId) {
        List<World> worlds = this.jdbcTemplate.query("SELECT state_json FROM worlds WHERE id = ?", (resultSet, rowNumber) -> this.read(resultSet.getString(1)), worldId.toString());
        return worlds.stream().findFirst();
    }

    @Override
    public Optional<World> findAtSequence(UUID worldId, long sequenceNumber) {
        List<World> worlds = this.jdbcTemplate.query("SELECT state_json FROM world_snapshots WHERE world_id = ? AND sequence_number <= ? ORDER BY sequence_number DESC LIMIT 1", (resultSet, rowNumber) -> this.read(resultSet.getString(1)), worldId.toString(), sequenceNumber);
        return worlds.stream().findFirst();
    }

    @Override
    public List<World> findAll() {
        return this.jdbcTemplate.query("SELECT state_json FROM worlds ORDER BY updated_at DESC", (resultSet, rowNumber) -> this.read(resultSet.getString(1)));
    }

    @Override
    public void deleteAll() {
        this.jdbcTemplate.update("DELETE FROM world_snapshots");
        this.jdbcTemplate.update("DELETE FROM worlds");
    }

    private String write(World world) {
        try { return this.objectMapper.writeValueAsString(world); } catch (JsonProcessingException exception) { throw new IllegalStateException("Impossible de sérialiser le monde", exception); }
    }

    private World read(String json) {
        try { return this.objectMapper.readValue(json, World.class); } catch (JsonProcessingException exception) { throw new IllegalStateException("Impossible de désérialiser le monde", exception); }
    }
}
