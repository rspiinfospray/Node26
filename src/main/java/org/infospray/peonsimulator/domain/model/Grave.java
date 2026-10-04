package org.infospray.peonsimulator.domain.model;

import java.util.UUID;

public record Grave(UUID peonId, HexCoordinate position, int deathRound, long deathSequence) {
}
