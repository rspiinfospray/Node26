package org.infospray.peonsimulator.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Peon {
    private UUID id;
    private String firstName;
    private UUID teamId;
    private int healthPoints = 100;
    private int maxHealthPoints = 100;
    private int experiencePoints;
    private int experiencePerLevel = 100;
    private int level = 1;
    private int attackDamage = 20;
    private HexCoordinate position;
    private boolean alive = true;
    private Map<String, RememberedCell> mentalMap = new LinkedHashMap<>();

    public Peon() {
    }

    public Peon(UUID id, String firstName, UUID teamId, HexCoordinate position) {
        this.id = id;
        this.firstName = firstName;
        this.teamId = teamId;
        this.position = position;
    }

    public UUID getId() { return this.id; }
    public String getFirstName() { return this.firstName; }
    public UUID getTeamId() { return this.teamId; }
    public int getHealthPoints() { return this.healthPoints; }
    public int getMaxHealthPoints() { return this.maxHealthPoints; }
    public int getExperiencePoints() { return this.experiencePoints; }
    public int getExperiencePerLevel() { return this.experiencePerLevel; }
    public int getLevel() { return this.level; }
    public int getAttackDamage() { return this.attackDamage; }
    public HexCoordinate getPosition() { return this.position; }
    public boolean isAlive() { return this.alive; }
    public Map<String, RememberedCell> getMentalMap() { return this.mentalMap; }
    public void setId(UUID id) { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }
    public void setHealthPoints(int value) { this.healthPoints = Math.max(0, Math.min(this.maxHealthPoints, value)); this.alive = this.healthPoints > 0; }
    public void setMaxHealthPoints(int value) { this.maxHealthPoints = Math.max(1, value); this.healthPoints = Math.min(this.healthPoints, this.maxHealthPoints); }
    public void setExperiencePoints(int value) { this.experiencePoints = Math.max(0, value); this.level = 1 + this.experiencePoints / this.experiencePerLevel; }
    public void setExperiencePerLevel(int value) { this.experiencePerLevel = Math.max(1, value); this.level = 1 + this.experiencePoints / this.experiencePerLevel; }
    public void setLevel(int level) { this.level = level; }
    public void setAttackDamage(int value) { this.attackDamage = Math.max(0, value); }
    public void setPosition(HexCoordinate position) { this.position = position; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public void setMentalMap(Map<String, RememberedCell> mentalMap) { this.mentalMap = new LinkedHashMap<>(mentalMap); }
    public void heal(int amount) { this.setHealthPoints(this.healthPoints + amount); }
    public void hurt(int amount) { this.setHealthPoints(this.healthPoints - amount); }
    public int gainExperience(int amount, int maxHealthGainPerLevel, int attackDamageGainPerLevel) {
        int previous = this.level;
        this.setExperiencePoints(this.experiencePoints + amount);
        int levelsGained = this.level - previous;
        if (levelsGained > 0) {
            this.setMaxHealthPoints(this.maxHealthPoints + levelsGained * maxHealthGainPerLevel);
            this.setAttackDamage(this.attackDamage + levelsGained * attackDamageGainPerLevel);
        }
        return previous;
    }

    public void remember(Cell cell, boolean visited, long sequence) {
        this.remember(cell, visited, sequence, List.of());
    }

    public void remember(Cell cell, boolean visited, long sequence, List<RememberedPeonObservation> rememberedPeons) {
        String key = cell.getCoordinate().q() + ":" + cell.getCoordinate().r();
        RememberedCell remembered = this.mentalMap.get(key);
        if (remembered == null) {
            this.mentalMap.put(key, new RememberedCell(cell, visited, sequence, rememberedPeons));
        } else {
            remembered.refresh(cell, visited, sequence, rememberedPeons);
        }
    }
}
