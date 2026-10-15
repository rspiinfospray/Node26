package org.infospray.peonsimulator.domain.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class World {
    private UUID id;
    private String name;
    private WorldStatus status = WorldStatus.CREATED;
    private int width;
    private int height;
    private double rockPercentage;
    private double treePercentage;
    private double foodCellPercentage;
    private int minFoodPerCell;
    private int maxFoodPerCell;
    private int sheepCount;
    private int hungerHealthLossPerTurn;
    private int maxRounds;
    private long seed;
    private int currentRound = 1;
    private int currentActorIndex;
    private long sequenceNumber;
    private UUID pendingActionId;
    private Map<String, Cell> cells = new LinkedHashMap<>();
    private Map<UUID, Team> teams = new LinkedHashMap<>();
    private Map<UUID, Peon> peons = new LinkedHashMap<>();
    private Map<UUID, Grave> graves = new LinkedHashMap<>();
    private Map<UUID, House> houses = new LinkedHashMap<>();
    private Map<UUID, WoodBundle> woodBundles = new LinkedHashMap<>();
    private Map<UUID, Sheep> sheep = new LinkedHashMap<>();
    private Map<UUID, WheatField> wheatFields = new LinkedHashMap<>();
    private Map<UUID, Granary> granaries = new LinkedHashMap<>();
    private transient Map<String, House> housesByPosition = Map.of();
    private transient Map<String, WheatField> wheatFieldsByPosition = Map.of();
    private transient Map<String, Granary> granariesByPosition = Map.of();
    private transient int indexedHouseCount = -1;
    private transient int indexedWheatFieldCount = -1;
    private transient int indexedGranaryCount = -1;

    public World() {
    }

    public UUID getId() { return this.id; }
    public String getName() { return this.name; }
    public WorldStatus getStatus() { return this.status; }
    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }
    public double getRockPercentage() { return this.rockPercentage; }
    public double getTreePercentage() { return this.treePercentage; }
    public double getFoodCellPercentage() { return this.foodCellPercentage; }
    public int getMinFoodPerCell() { return this.minFoodPerCell; }
    public int getMaxFoodPerCell() { return this.maxFoodPerCell; }
    public int getSheepCount() { return this.sheepCount; }
    public int getHungerHealthLossPerTurn() { return this.hungerHealthLossPerTurn; }
    public int getMaxRounds() { return this.maxRounds; }
    public long getSeed() { return this.seed; }
    public int getCurrentRound() { return this.currentRound; }
    public int getCurrentActorIndex() { return this.currentActorIndex; }
    public long getSequenceNumber() { return this.sequenceNumber; }
    public UUID getPendingActionId() { return this.pendingActionId; }
    public Map<String, Cell> getCells() { return this.cells; }
    public Map<UUID, Team> getTeams() { return this.teams; }
    public Map<UUID, Peon> getPeons() { return this.peons; }
    public Map<UUID, Grave> getGraves() { return this.graves; }
    public Map<UUID, House> getHouses() { return this.houses; }
    public Map<UUID, WoodBundle> getWoodBundles() { return this.woodBundles; }
    public Map<UUID, Sheep> getSheep() { return this.sheep; }
    public Map<UUID, WheatField> getWheatFields() { return this.wheatFields; }
    public Map<UUID, Granary> getGranaries() { return this.granaries; }
    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setStatus(WorldStatus status) { this.status = status; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setRockPercentage(double value) { this.rockPercentage = value; }
    public void setTreePercentage(double value) { this.treePercentage = value; }
    public void setFoodCellPercentage(double value) { this.foodCellPercentage = value; }
    @JsonSetter("rockRatio")
    public void loadLegacyRockRatio(double value) { this.rockPercentage = value * 100; }
    @JsonSetter("treeRatio")
    public void loadLegacyTreeRatio(double value) { this.treePercentage = value * 100; }
    @JsonSetter("foodCellRatio")
    public void loadLegacyFoodCellRatio(double value) { this.foodCellPercentage = value * 100; }
    public void setMinFoodPerCell(int value) { this.minFoodPerCell = value; }
    public void setMaxFoodPerCell(int value) { this.maxFoodPerCell = value; }
    public void setSheepCount(int value) { this.sheepCount = Math.max(0, value); }
    public void setHungerHealthLossPerTurn(int value) { this.hungerHealthLossPerTurn = value; }
    public void setMaxRounds(int value) { this.maxRounds = value; }
    public void setSeed(long seed) { this.seed = seed; }
    public void setCurrentRound(int value) { this.currentRound = value; }
    public void setCurrentActorIndex(int value) { this.currentActorIndex = value; }
    public void setSequenceNumber(long value) { this.sequenceNumber = value; }
    public void setPendingActionId(UUID value) { this.pendingActionId = value; }
    public void setCells(Map<String, Cell> cells) { this.cells = new LinkedHashMap<>(cells); }
    public void setTeams(Map<UUID, Team> teams) { this.teams = new LinkedHashMap<>(teams); }
    public void setPeons(Map<UUID, Peon> peons) {
        this.peons = new LinkedHashMap<>(peons);
        this.peons.values().stream().filter(peon -> !peon.isAlive()).forEach(peon -> this.graves.putIfAbsent(peon.getId(), new Grave(peon.getId(), peon.getPosition(), this.currentRound, this.sequenceNumber)));
    }
    public void setGraves(Map<UUID, Grave> graves) {
        if (graves != null) { this.graves.putAll(graves); }
    }
    public void setHouses(Map<UUID, House> value) { this.houses = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value); this.indexedHouseCount = -1; }
    public void setWoodBundles(Map<UUID, WoodBundle> value) { this.woodBundles = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value); }
    public void setSheep(Map<UUID, Sheep> value) { this.sheep = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value); }
    public void setWheatFields(Map<UUID, WheatField> value) { this.wheatFields = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value); this.indexedWheatFieldCount = -1; }
    public void setGranaries(Map<UUID, Granary> value) { this.granaries = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value); this.indexedGranaryCount = -1; }
    public House houseAt(HexCoordinate coordinate) { this.refreshHouseIndex(); return this.housesByPosition.get(key(coordinate)); }
    public WheatField wheatFieldAt(HexCoordinate coordinate) { this.refreshWheatFieldIndex(); return this.wheatFieldsByPosition.get(key(coordinate)); }
    public Granary granaryAt(HexCoordinate coordinate) { this.refreshGranaryIndex(); return this.granariesByPosition.get(key(coordinate)); }
    public boolean hasAlliedGranaryWithin(HexCoordinate coordinate, UUID teamId, int maximumDistance) { return this.granaries.values().stream().anyMatch(granary -> granary.getTeamId().equals(teamId) && granary.getPosition().distanceTo(coordinate) <= maximumDistance); }
    public Granary closestAlliedGranaryWithin(HexCoordinate coordinate, UUID teamId, int maximumDistance) { return this.granaries.values().stream().filter(granary -> granary.getTeamId().equals(teamId) && granary.getPosition().distanceTo(coordinate) <= maximumDistance).min(java.util.Comparator.comparingInt(granary -> granary.getPosition().distanceTo(coordinate))).orElse(null); }

    public Cell cell(HexCoordinate coordinate) { return this.cells.get(key(coordinate)); }
    public boolean contains(HexCoordinate coordinate) { return this.cells.containsKey(key(coordinate)); }
    public void addCell(Cell cell) { this.cells.put(key(cell.getCoordinate()), cell); }
    public List<Peon> alivePeons() { return this.peons.values().stream().filter(Peon::isAlive).toList(); }

    public Peon nextActor() {
        if (this.alivePeons().isEmpty()) {
            this.status = WorldStatus.FINISHED;
            return null;
        }
        List<Peon> ordered = new ArrayList<>(this.peons.values());
        while (true) {
            if (this.currentActorIndex >= ordered.size()) {
                this.currentActorIndex = 0;
                this.currentRound++;
            }
            if (this.currentRound > this.maxRounds) {
                this.status = WorldStatus.FINISHED;
                return null;
            }
            Peon candidate = ordered.get(this.currentActorIndex);
            if (candidate.isAlive()) {
                return candidate;
            }
            this.currentActorIndex++;
        }
    }

    public void completeTurn() {
        this.currentActorIndex++;
        this.pendingActionId = null;
        if (this.currentActorIndex >= this.peons.size()) {
            this.currentActorIndex = 0;
            this.currentRound++;
            if (this.currentRound > this.maxRounds) {
                this.status = WorldStatus.FINISHED;
            }
        }
    }

    public static String key(HexCoordinate coordinate) { return coordinate.q() + ":" + coordinate.r(); }

    private void refreshHouseIndex() {
        if (this.indexedHouseCount == this.houses.size()) { return; }
        Map<String, House> index = new LinkedHashMap<>();
        this.houses.values().forEach(house -> index.put(key(house.getPosition()), house));
        this.housesByPosition = index;
        this.indexedHouseCount = this.houses.size();
    }

    private void refreshWheatFieldIndex() {
        if (this.indexedWheatFieldCount == this.wheatFields.size()) { return; }
        Map<String, WheatField> index = new LinkedHashMap<>();
        this.wheatFields.values().forEach(field -> index.put(key(field.getPosition()), field));
        this.wheatFieldsByPosition = index;
        this.indexedWheatFieldCount = this.wheatFields.size();
    }

    private void refreshGranaryIndex() {
        if (this.indexedGranaryCount == this.granaries.size()) { return; }
        Map<String, Granary> index = new LinkedHashMap<>();
        this.granaries.values().forEach(granary -> index.put(key(granary.getPosition()), granary));
        this.granariesByPosition = index;
        this.indexedGranaryCount = this.granaries.size();
    }
}
