package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.PeonDiagnosticResponse;
import org.infospray.peonsimulator.application.dto.PeonDiagnosticSummary;
import org.infospray.peonsimulator.application.dto.EventQuery;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

@Component
public class SummarizePeonDiagnosticCommand {
    private final EventRepository eventRepository;
    public SummarizePeonDiagnosticCommand(EventRepository eventRepository) { this.eventRepository = eventRepository; }

    public PeonDiagnosticSummary execute(UUID worldId, UUID peonId, Integer fromRound, Integer toRound) {
        List<SimulationEvent> events = this.eventRepository.find(new EventQuery(worldId, 0, 2000, peonId, fromRound, toRound, Set.of()));
        List<PeonDiagnosticResponse.Turn> turns = events.stream().filter(event -> event.eventType().equals("PEON_DECISION_MADE")).map(event -> new PeonDiagnosticResponse.Turn(event.roundNumber(), event.sequenceNumber(), event.payload(), events.stream().filter(outcome -> outcome.sequenceNumber() == event.sequenceNumber() && !outcome.eventType().equals("PEON_DECISION_MADE")).map(outcome -> Map.<String, Object>of("eventType", outcome.eventType(), "payload", outcome.payload())).toList())).toList();
        List<PeonDiagnosticSummary.Anomaly> anomalies = new ArrayList<>();
        this.detectRepeatedCriticalActions(turns, anomalies);
        this.detectOscillation(turns, anomalies);
        turns.stream().filter(turn -> turn.outcomes().stream().anyMatch(outcome -> outcome.get("eventType").equals("PEON_DIED"))).forEach(turn -> anomalies.add(new PeonDiagnosticSummary.Anomaly("DEATH", List.of(turn.round()), Map.of("sequence", turn.sequence()))));
        return new PeonDiagnosticSummary(worldId, peonId, anomalies);
    }

    private void detectRepeatedCriticalActions(List<PeonDiagnosticResponse.Turn> turns, List<PeonDiagnosticSummary.Anomaly> anomalies) {
        List<Integer> rounds = turns.stream().filter(turn -> String.valueOf(turn.decision().get("situationKey")).startsWith("PV_CRITIQUES")).filter(turn -> List.of("VOIR", "NE_RIEN_FAIRE").contains(String.valueOf(turn.decision().get("actionType")))).map(PeonDiagnosticResponse.Turn::round).toList();
        if (!rounds.isEmpty()) { anomalies.add(new PeonDiagnosticSummary.Anomaly("NON_SURVIVAL_ACTION_AT_CRITICAL_HEALTH", rounds, Map.of("count", rounds.size()))); }
    }

    private void detectOscillation(List<PeonDiagnosticResponse.Turn> turns, List<PeonDiagnosticSummary.Anomaly> anomalies) {
        List<String> destinations = turns.stream().map(turn -> turn.decision().get("destination")).filter(java.util.Objects::nonNull).map(Object::toString).toList();
        for (int index = 3; index < destinations.size(); index++) {
            if (destinations.get(index).equals(destinations.get(index - 2)) && destinations.get(index - 1).equals(destinations.get(index - 3))) { anomalies.add(new PeonDiagnosticSummary.Anomaly("MOVEMENT_OSCILLATION", List.of(), new LinkedHashMap<>(Map.of("positions", destinations.subList(index - 3, index + 1))))); return; }
        }
    }
}
