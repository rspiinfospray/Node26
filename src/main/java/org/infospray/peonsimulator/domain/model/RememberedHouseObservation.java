package org.infospray.peonsimulator.domain.model;

import java.util.UUID;

public record RememberedHouseObservation(UUID houseId, HexCoordinate position, UUID hostTeamId, boolean occupied, long observedAtSequence) {
}
