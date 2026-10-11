package org.infospray.peonsimulator.domain.model;

import java.util.UUID;

public class Granary {
    private UUID id;
    private HexCoordinate position;
    private UUID teamId;
    private int wheatQuantity;

    public Granary() {
    }

    public Granary(UUID id, HexCoordinate position, UUID teamId) {
        this.id = id;
        this.position = position;
        this.teamId = teamId;
    }

    public UUID getId() { return this.id; }
    public HexCoordinate getPosition() { return this.position; }
    public UUID getTeamId() { return this.teamId; }
    public int getWheatQuantity() { return this.wheatQuantity; }
    public void setId(UUID value) { this.id = value; }
    public void setPosition(HexCoordinate value) { this.position = value; }
    public void setTeamId(UUID value) { this.teamId = value; }
    public void setWheatQuantity(int value) { this.wheatQuantity = Math.max(0, value); }
    public int store(int quantity, int capacity) { int stored = Math.min(Math.max(0, quantity), Math.max(0, capacity - this.wheatQuantity)); this.wheatQuantity += stored; return stored; }
    public boolean consume() { if (this.wheatQuantity <= 0) { return false; } this.wheatQuantity--; return true; }
}
