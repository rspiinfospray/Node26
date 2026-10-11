package org.infospray.peonsimulator.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PeonDiagnosticSummary(UUID worldId, UUID peonId, List<Anomaly> anomalies) {
    public record Anomaly(String type, List<Integer> rounds, Map<String, Object> details) { }
}
