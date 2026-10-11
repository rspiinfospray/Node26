package org.infospray.peonsimulator.application.port.secondary;

import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.application.dto.EventQuery;

import java.util.List;
import java.util.UUID;

public interface EventRepository {
    void appendAll(List<SimulationEvent> events);
    List<SimulationEvent> findByWorldId(UUID worldId);
    List<SimulationEvent> find(EventQuery query);
    boolean actionWasProcessed(UUID actionId);
    void markActionProcessed(UUID actionId);
    void deleteAll();
}
