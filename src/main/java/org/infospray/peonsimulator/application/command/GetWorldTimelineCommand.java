package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.EventQuery;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GetWorldTimelineCommand {
    private final EventRepository eventRepository;
    public GetWorldTimelineCommand(EventRepository eventRepository) { this.eventRepository = eventRepository; }
    public List<SimulationEvent> execute(EventQuery query) { return this.eventRepository.find(query); }
}
