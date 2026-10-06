package org.infospray.peonsimulator.domain.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Cell {
    private HexCoordinate coordinate;
    private TerrainType terrain;
    private int foodQuantity;
    private int treeHealthPoints;
    private Set<UUID> occupantPeonIds = new LinkedHashSet<>();

    public Cell() {
    }

    public Cell(HexCoordinate coordinate, TerrainType terrain, int foodQuantity) {
        this.coordinate = coordinate;
        this.terrain = terrain;
        this.foodQuantity = foodQuantity;
        this.treeHealthPoints = terrain == TerrainType.TREE ? 100 : 0;
    }

    public HexCoordinate getCoordinate() { return this.coordinate; }
    public TerrainType getTerrain() { return this.terrain; }
    public int getFoodQuantity() { return this.foodQuantity; }
    public int getTreeHealthPoints() { return this.treeHealthPoints; }
    public Set<UUID> getOccupantPeonIds() { return this.occupantPeonIds; }
    public void setCoordinate(HexCoordinate coordinate) { this.coordinate = coordinate; }
    public void setTerrain(TerrainType terrain) { this.terrain = terrain; if (terrain == TerrainType.TREE && this.treeHealthPoints <= 0) { this.treeHealthPoints = 100; } if (terrain != TerrainType.TREE) { this.treeHealthPoints = 0; } }
    public void setFoodQuantity(int foodQuantity) { this.foodQuantity = Math.max(0, foodQuantity); }
    public void setTreeHealthPoints(int value) { this.treeHealthPoints = this.terrain == TerrainType.TREE ? Math.max(0, value) : 0; }
    public void setOccupantPeonIds(Set<UUID> occupantPeonIds) { this.occupantPeonIds = new LinkedHashSet<>(occupantPeonIds); }
    public void addOccupant(UUID peonId) { this.occupantPeonIds.add(peonId); }
    public void removeOccupant(UUID peonId) { this.occupantPeonIds.remove(peonId); }
    public void consumeFood() { this.foodQuantity = Math.max(0, this.foodQuantity - 1); }
    public int chopTree(int amount) { if (this.terrain != TerrainType.TREE || this.treeHealthPoints <= 0) { return 0; } int harvested = Math.min(amount, this.treeHealthPoints); this.treeHealthPoints -= harvested; if (this.treeHealthPoints == 0) { this.setTerrain(TerrainType.PLAIN); } return harvested; }
}
