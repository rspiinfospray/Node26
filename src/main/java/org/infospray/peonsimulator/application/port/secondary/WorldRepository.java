package org.infospray.peonsimulator.application.port.secondary;

import org.infospray.peonsimulator.domain.model.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorldRepository {
    World save(World world);
    void saveSnapshot(World world);
    Optional<World> findById(UUID worldId);
    Optional<World> findAtSequence(UUID worldId, long sequenceNumber);
    List<World> findAll();
    void deleteAll();
}
