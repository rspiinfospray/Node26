package org.infospray.peonsimulator.domain.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Cell {
    private HexCoordinate coordinate;
    private TerrainType terrain;
    private int foodQuantity;
    private Set<UUID> occupantPeonIds = new LinkedHashSet<>();

    public Cell() {
    }

    public Cell(HexCoordinate coordinate, TerrainType terrain, int foodQuantity) {
        this.coordinate = coordinate;
        this.terrain = terrain;
        this.foodQuantity = foodQuantity;
    }

    public HexCoordinate getCoordinate() { return this.coordinate; }
    public TerrainType getTerrain() { return this.terrain; }
    public int getFoodQuantity() { return this.foodQuantity; }
    public Set<UUID> getOccupantPeonIds() { return this.occupantPeonIds; }
    public void setCoordinate(HexCoordinate coordinate) { this.coordinate = coordinate; }
    public void setTerrain(TerrainType terrain) { this.terrain = terrain; }
    public void setFoodQuantity(int foodQuantity) { this.foodQuantity = Math.max(0, foodQuantity); }
    public void setOccupantPeonIds(Set<UUID> occupantPeonIds) { this.occupantPeonIds = new LinkedHashSet<>(occupantPeonIds); }
    public void addOccupant(UUID peonId) { this.occupantPeonIds.add(peonId); }
    public void removeOccupant(UUID peonId) { this.occupantPeonIds.remove(peonId); }
    public void consumeFood() { this.foodQuantity = Math.max(0, this.foodQuantity - 1); }
}
