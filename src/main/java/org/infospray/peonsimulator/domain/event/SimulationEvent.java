package org.infospray.peonsimulator.domain.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record SimulationEvent(UUID eventId, UUID actionId, String eventType, int eventVersion, Instant occurredAt, UUID worldId, UUID teamId, UUID peonId, int roundNumber, long sequenceNumber, int eventIndex, UUID correlationId, UUID causationId, Map<String, Object> payload) {
    public static SimulationEvent of(UUID actionId, String type, UUID worldId, UUID teamId, UUID peonId, int round, long sequence, int index, Map<String, Object> payload) {
        return new SimulationEvent(UUID.randomUUID(), actionId, type, 1, Instant.now(), worldId, teamId, peonId, round, sequence, index, actionId, actionId, new LinkedHashMap<>(payload));
    }
}
