package org.infospray.peonsimulator.domain.model;

import java.util.UUID;

public class Sheep {
    private UUID id;
    private HexCoordinate position;

    public Sheep() {
    }

    public Sheep(UUID id, HexCoordinate position) {
        this.id = id;
        this.position = position;
    }

    public UUID getId() { return this.id; }
    public HexCoordinate getPosition() { return this.position; }
    public void setId(UUID id) { this.id = id; }
    public void setPosition(HexCoordinate position) { this.position = position; }
}
