package org.infospray.peonsimulator.application.port.secondary;

import org.infospray.peonsimulator.domain.model.World;
import org.infospray.peonsimulator.domain.model.WorldStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorldRepository {
    World save(World world);
    World saveWithSnapshot(World world);
    void saveSnapshot(World world);
    Optional<World> findById(UUID worldId);
    Optional<World> findAtSequence(UUID worldId, long sequenceNumber);
    List<World> findAll();
    List<UUID> findIdsByStatus(WorldStatus status);
    void deleteAll();
}
