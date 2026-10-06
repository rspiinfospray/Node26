package org.infospray.peonsimulator.domain.model;

import java.util.UUID;

public record WoodBundle(UUID id, HexCoordinate position, int quantity) {
}
