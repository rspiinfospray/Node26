package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.EventQuery;
import org.infospray.peonsimulator.application.dto.PeonDiagnosticResponse;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GetPeonDiagnosticCommand {
    private final WorldRepository worldRepository;
    private final EventRepository eventRepository;

    public GetPeonDiagnosticCommand(WorldRepository worldRepository, EventRepository eventRepository) { this.worldRepository = worldRepository; this.eventRepository = eventRepository; }

    public PeonDiagnosticResponse execute(UUID worldId, UUID peonId, Integer fromRound, Integer toRound) {
        World world = this.worldRepository.findById(worldId).orElseThrow(() -> new IllegalArgumentException("Monde introuvable: " + worldId));
        Peon peon = world.getPeons().get(peonId);
        if (peon == null) { throw new IllegalArgumentException("Peon introuvable: " + peonId); }
        List<SimulationEvent> events = this.eventRepository.find(new EventQuery(worldId, 0, 2000, peonId, fromRound, toRound, Set.of()));
        Map<Long, List<SimulationEvent>> eventsBySequence = events.stream().collect(Collectors.groupingBy(SimulationEvent::sequenceNumber));
        List<PeonDiagnosticResponse.Turn> turns = events.stream().filter(event -> event.eventType().equals("PEON_DECISION_MADE")).map(decision -> this.turn(decision, eventsBySequence.getOrDefault(decision.sequenceNumber(), List.of()))).toList();
        return new PeonDiagnosticResponse(worldId, world.getName(), peonId, peon.getFirstName(), turns);
    }

    private PeonDiagnosticResponse.Turn turn(SimulationEvent decision, List<SimulationEvent> sequenceEvents) {
        List<Map<String, Object>> outcomes = sequenceEvents.stream().filter(event -> !event.eventType().equals("PEON_DECISION_MADE")).map(event -> Map.<String, Object>of("eventType", event.eventType(), "payload", event.payload())).toList();
        return new PeonDiagnosticResponse.Turn(decision.roundNumber(), decision.sequenceNumber(), decision.payload(), outcomes);
    }
}
