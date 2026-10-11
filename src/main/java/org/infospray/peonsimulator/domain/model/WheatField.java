package org.infospray.peonsimulator.domain.model;

import java.util.UUID;

public class WheatField {
    private UUID id;
    private HexCoordinate position;
    private UUID teamId;
    private int wheatQuantity;
    private int readyAtRound;

    public WheatField() {
    }

    public WheatField(UUID id, HexCoordinate position, UUID teamId, int readyAtRound) {
        this.id = id;
        this.position = position;
        this.teamId = teamId;
        this.readyAtRound = readyAtRound;
    }

    public UUID getId() { return this.id; }
    public HexCoordinate getPosition() { return this.position; }
    public UUID getTeamId() { return this.teamId; }
    public int getWheatQuantity() { return this.wheatQuantity; }
    public int getReadyAtRound() { return this.readyAtRound; }
    public void setId(UUID value) { this.id = value; }
    public void setPosition(HexCoordinate value) { this.position = value; }
    public void setTeamId(UUID value) { this.teamId = value; }
    public void setWheatQuantity(int value) { this.wheatQuantity = Math.max(0, value); }
    public void setReadyAtRound(int value) { this.readyAtRound = value; }
    public boolean isReady() { return this.wheatQuantity > 0; }
    public void ripen(int currentRound, int yield) { if (!this.isReady() && currentRound >= this.readyAtRound) { this.wheatQuantity = yield; } }
    public int harvest(int maximum, int currentRound, int growthRounds) { int harvested = Math.min(this.wheatQuantity, Math.max(0, maximum)); this.wheatQuantity -= harvested; if (this.wheatQuantity == 0) { this.readyAtRound = currentRound + growthRounds; } return harvested; }
}
