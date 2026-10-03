package org.infospray.peonsimulator.domain.model;

import java.util.UUID;

public record RememberedPeonObservation(UUID peonId, UUID teamId, PeonRelation relation, HexCoordinate position, int healthPoints, int level, long observedAtSequence) {
}
