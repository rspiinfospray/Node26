package org.infospray.peonsimulator.application.dto;

import java.util.Set;
import java.util.UUID;

public record EventQuery(UUID worldId, long afterSequence, int limit, UUID peonId, Integer fromRound, Integer toRound, Set<String> eventTypes) {
    public int cappedLimit() { return Math.max(1, Math.min(this.limit, 2000)); }
}
