package org.infospray.peonsimulator.application.dto;

import org.infospray.peonsimulator.domain.model.WorldStatus;

import java.util.UUID;

public record WorldAnalysisContextResponse(UUID id, String name, WorldStatus status, Dimensions dimensions, Generation generation, Simulation simulation, Population population, Constructions constructions, int remainingSheep) {
    public record Dimensions(int width, int height) {
    }

    public record Generation(long seed, double rockPercentage, double treePercentage, double foodCellPercentage, int initialSheep, boolean riverGenerationEnabled) {
    }

    public record Simulation(int currentRound, int maxRounds, long sequenceNumber) {
    }

    public record Population(int totalPeons, int alivePeons, int deadPeons) {
    }

    public record Constructions(int houses, int granaries, int fields) {
    }
}
