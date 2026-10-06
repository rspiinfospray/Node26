package org.infospray.peonsimulator.application.dto;

import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.ActionKnowledge;
import org.infospray.peonsimulator.domain.model.PeonPersonality;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.TerrainType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PeonDecisionContext(UUID peonId, UUID teamId, int healthPoints, int maxHealthPoints, int experiencePoints, int level, HexCoordinate position, boolean concealedByTree, long sequenceNumber, long decisionSeed, PeonPersonality personality, Map<String, ActionKnowledge> learnedActions, Map<String, RememberedCell> mentalMap, List<ObservedPeon> peonsOnCurrentCell, int foodOnCurrentCell, List<HexCoordinate> traversableNeighbors, int currentRound, int lastObservationRound, TerrainType currentTerrain, int treeHealthPoints, int woodQuantity, boolean buildable, ObservedHouse house) {
    public PeonDecisionContext(
            UUID peonId, UUID teamId, int healthPoints, int maxHealthPoints, int experiencePoints, int level,
            HexCoordinate position, boolean concealedByTree, long sequenceNumber, long decisionSeed, PeonPersonality personality,
            Map<String, ActionKnowledge> learnedActions, Map<String, RememberedCell> mentalMap, List<ObservedPeon> peonsOnCurrentCell,
            int foodOnCurrentCell, List<HexCoordinate> traversableNeighbors, int currentRound, int lastObservationRound) {
        this(peonId, teamId, healthPoints, maxHealthPoints, experiencePoints, level, position, concealedByTree, sequenceNumber, decisionSeed, personality, learnedActions, mentalMap, peonsOnCurrentCell, foodOnCurrentCell, traversableNeighbors, currentRound, lastObservationRound, concealedByTree ? TerrainType.TREE : TerrainType.PLAIN, 0, 0, false, null);
    }

    public record ObservedPeon(UUID id, UUID teamId, int healthPoints, int level) {
    }

    public record ObservedHouse(UUID id, UUID hostPeonId, UUID hostTeamId, boolean actorInside, boolean occupied) {
    }
}
