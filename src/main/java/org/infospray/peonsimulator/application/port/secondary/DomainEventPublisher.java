package org.infospray.peonsimulator.application.port.secondary;

import org.infospray.peonsimulator.domain.event.SimulationEvent;

import java.util.List;

public interface DomainEventPublisher {
    void publish(List<SimulationEvent> events);
}
