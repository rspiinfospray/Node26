package org.infospray.peonsimulator.domain.model;

import java.util.UUID;

public record StrategyCommitment(UUID peonId, StrategyType type, int observedAtRound) {
}
