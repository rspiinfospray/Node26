package org.infospray.peonsimulator.application.dto;

import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.RememberedCell;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PeonDecisionContext(UUID peonId, UUID teamId, int healthPoints, int experiencePoints, int level, HexCoordinate position, boolean concealedByTree, long sequenceNumber, long decisionSeed, Map<String, RememberedCell> mentalMap, List<ObservedPeon> peonsOnCurrentCell, int foodOnCurrentCell, List<HexCoordinate> traversableNeighbors) {
    public record ObservedPeon(UUID id, UUID teamId, int healthPoints, int level) {
    }
}
