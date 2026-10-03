package org.infospray.peonsimulator.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateWorldRequest(@Min(5) @Max(150) Integer width, @Min(5) @Max(150) Integer height, @Min(0) @Max(60) Integer rockPercentage, @Min(0) @Max(60) Integer treePercentage, @Min(0) @Max(100) Integer foodCellPercentage, @Min(1) Integer minFoodPerCell, @Min(1) Integer maxFoodPerCell, @Min(1) Integer hungerHealthLossPerTurn, @Min(1) Integer maxRounds, Long seed, @NotEmpty List<@Valid TeamRequest> teams) {
    public record TeamRequest(@NotBlank String name, @Min(1) @Max(100) int peonCount) {
    }

    public int resolvedWidth() { return this.width == null ? 50 : this.width; }
    public int resolvedHeight() { return this.height == null ? 50 : this.height; }
    public int resolvedRockPercentage() { return this.rockPercentage == null ? 20 : this.rockPercentage; }
    public int resolvedTreePercentage() { return this.treePercentage == null ? 15 : this.treePercentage; }
    public int resolvedFoodCellPercentage() { return this.foodCellPercentage == null ? 15 : this.foodCellPercentage; }
    public int resolvedMinFood() { return this.minFoodPerCell == null ? 1 : this.minFoodPerCell; }
    public int resolvedMaxFood() { return this.maxFoodPerCell == null ? 3 : this.maxFoodPerCell; }
    public int resolvedHungerLoss() { return this.hungerHealthLossPerTurn == null ? 5 : this.hungerHealthLossPerTurn; }
    public int resolvedMaxRounds() { return this.maxRounds == null ? 100 : this.maxRounds; }
    public long resolvedSeed() { return this.seed == null ? System.currentTimeMillis() : this.seed; }

    @AssertTrue(message = "La somme des pourcentages de roches et d'arbres doit être inférieure à 100 %")
    public boolean isTerrainPercentageValid() { return this.resolvedRockPercentage() + this.resolvedTreePercentage() < 100; }
}
