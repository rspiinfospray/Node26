package org.infospray.peonsimulator.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PeonDiagnosticResponse(UUID worldId, String worldName, UUID peonId, String peonName, List<Turn> turns) {
    public record Turn(int round, long sequence, Map<String, Object> decision, List<Map<String, Object>> outcomes) { }
}
