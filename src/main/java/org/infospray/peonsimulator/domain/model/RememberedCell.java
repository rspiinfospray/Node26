package org.infospray.peonsimulator.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RememberedCell {
    private HexCoordinate coordinate;
    private TerrainType terrain;
    private boolean visited;
    private Long firstVisitedAtSequence;
    private Long lastVisitedAtSequence;
    private long lastObservedAtSequence;
    private int rememberedFoodQuantity;
    private List<UUID> rememberedOccupants = new ArrayList<>();
    private List<RememberedPeonObservation> rememberedPeons = new ArrayList<>();
    private List<RememberedGraveObservation> rememberedGraves = new ArrayList<>();

    public RememberedCell() {
    }

    public RememberedCell(Cell cell, boolean visited, long sequence) {
        this(cell, visited, sequence, List.of());
    }

    public RememberedCell(Cell cell, boolean visited, long sequence, List<RememberedPeonObservation> rememberedPeons) {
        this(cell, visited, sequence, rememberedPeons, List.of());
    }

    public RememberedCell(Cell cell, boolean visited, long sequence, List<RememberedPeonObservation> rememberedPeons, List<RememberedGraveObservation> rememberedGraves) {
        this.coordinate = cell.getCoordinate();
        this.terrain = cell.getTerrain();
        this.visited = visited;
        this.firstVisitedAtSequence = visited ? sequence : null;
        this.lastVisitedAtSequence = visited ? sequence : null;
        this.lastObservedAtSequence = sequence;
        this.rememberedFoodQuantity = cell.getFoodQuantity();
        this.rememberedOccupants = rememberedPeons.stream().map(RememberedPeonObservation::peonId).toList();
        this.rememberedPeons = new ArrayList<>(rememberedPeons);
        this.rememberedGraves = new ArrayList<>(rememberedGraves);
    }

    public HexCoordinate getCoordinate() { return this.coordinate; }
    public TerrainType getTerrain() { return this.terrain; }
    public boolean isVisited() { return this.visited; }
    public Long getFirstVisitedAtSequence() { return this.firstVisitedAtSequence; }
    public Long getLastVisitedAtSequence() { return this.lastVisitedAtSequence; }
    public long getLastObservedAtSequence() { return this.lastObservedAtSequence; }
    public int getRememberedFoodQuantity() { return this.rememberedFoodQuantity; }
    public List<UUID> getRememberedOccupants() { return this.rememberedOccupants; }
    public List<RememberedPeonObservation> getRememberedPeons() { return this.rememberedPeons; }
    public List<RememberedGraveObservation> getRememberedGraves() { return this.rememberedGraves; }
    public void setCoordinate(HexCoordinate coordinate) { this.coordinate = coordinate; }
    public void setTerrain(TerrainType terrain) { this.terrain = terrain; }
    public void setVisited(boolean visited) { this.visited = visited; }
    public void setFirstVisitedAtSequence(Long value) { this.firstVisitedAtSequence = value; }
    public void setLastVisitedAtSequence(Long value) { this.lastVisitedAtSequence = value; }
    public void setLastObservedAtSequence(long value) { this.lastObservedAtSequence = value; }
    public void setRememberedFoodQuantity(int value) { this.rememberedFoodQuantity = value; }
    public void setRememberedOccupants(List<UUID> value) { this.rememberedOccupants = new ArrayList<>(value); }
    public void setRememberedPeons(List<RememberedPeonObservation> value) { this.rememberedPeons = value == null ? new ArrayList<>() : new ArrayList<>(value); }
    public void setRememberedGraves(List<RememberedGraveObservation> value) { this.rememberedGraves = value == null ? new ArrayList<>() : new ArrayList<>(value); }

    public void refresh(Cell cell, boolean nowVisited, long sequence) {
        this.refresh(cell, nowVisited, sequence, List.of());
    }

    public void refresh(Cell cell, boolean nowVisited, long sequence, List<RememberedPeonObservation> rememberedPeons) {
        this.refresh(cell, nowVisited, sequence, rememberedPeons, List.of());
    }

    public void refresh(Cell cell, boolean nowVisited, long sequence, List<RememberedPeonObservation> rememberedPeons, List<RememberedGraveObservation> rememberedGraves) {
        this.terrain = cell.getTerrain();
        this.lastObservedAtSequence = sequence;
        this.rememberedFoodQuantity = cell.getFoodQuantity();
        this.rememberedOccupants = rememberedPeons.stream().map(RememberedPeonObservation::peonId).toList();
        this.rememberedPeons = new ArrayList<>(rememberedPeons);
        this.rememberedGraves = new ArrayList<>(rememberedGraves);
        if (nowVisited) {
            this.visited = true;
            this.firstVisitedAtSequence = this.firstVisitedAtSequence == null ? sequence : this.firstVisitedAtSequence;
            this.lastVisitedAtSequence = sequence;
        }
    }
}
