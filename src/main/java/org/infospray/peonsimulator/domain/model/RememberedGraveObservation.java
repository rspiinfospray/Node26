package org.infospray.peonsimulator.domain.model;

import java.util.UUID;

public record RememberedGraveObservation(UUID peonId, UUID teamId, PeonRelation relation, HexCoordinate position, int deathRound, long deathSequence, long observedAtSequence) {
}
