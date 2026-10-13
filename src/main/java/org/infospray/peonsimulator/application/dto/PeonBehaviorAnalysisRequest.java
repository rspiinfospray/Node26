package org.infospray.peonsimulator.application.dto;

import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.Peon;

import java.util.List;
import java.util.Map;

public record PeonBehaviorAnalysisRequest(Map<String, Object> worldContext, Map<String, Object> gameRules, Peon peonFinalState, List<SimulationEvent> diagnosticEvents, PeonDiagnosticSummary diagnosticSummary) {
}
