package org.infospray.peonsimulator.domain.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonPropertyOrder({"id", "firstName", "teamId", "maxHealthPoints", "healthPoints"})
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
    private PeonPersonality personality = new PeonPersonality();
    private List<ActionExperience> actionHistory = new ArrayList<>();
    private Map<String, ActionKnowledge> learnedActions = new LinkedHashMap<>();
    private String pendingSituationKey;
    private ActionType pendingDecisionAction;
    private int healthBeforeDecision;
    private int experienceBeforeDecision;
    private int knownCellsBeforeDecision;
    private int lastObservationRound;
    private Map<ItemType, Integer> inventory = new LinkedHashMap<>();
    private UUID insideHouseId;

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
    public PeonPersonality getPersonality() { return this.personality; }
    public List<ActionExperience> getActionHistory() { return this.actionHistory; }
    public Map<String, ActionKnowledge> getLearnedActions() { return this.learnedActions; }
    public String getPendingSituationKey() { return this.pendingSituationKey; }
    public ActionType getPendingDecisionAction() { return this.pendingDecisionAction; }
    public int getHealthBeforeDecision() { return this.healthBeforeDecision; }
    public int getExperienceBeforeDecision() { return this.experienceBeforeDecision; }
    public int getKnownCellsBeforeDecision() { return this.knownCellsBeforeDecision; }
    public int getLastObservationRound() { return this.lastObservationRound; }
    public Map<ItemType, Integer> getInventory() { return this.inventory; }
    public UUID getInsideHouseId() { return this.insideHouseId; }
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
    public void setPersonality(PeonPersonality value) { this.personality = value == null ? new PeonPersonality() : value; }
    public void setActionHistory(List<ActionExperience> value) { this.actionHistory = value == null ? new ArrayList<>() : new ArrayList<>(value); }
    public void setLearnedActions(Map<String, ActionKnowledge> value) { this.learnedActions = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value); }
    public void setPendingSituationKey(String value) { this.pendingSituationKey = value; }
    public void setPendingDecisionAction(ActionType value) { this.pendingDecisionAction = value; }
    public void setHealthBeforeDecision(int value) { this.healthBeforeDecision = value; }
    public void setExperienceBeforeDecision(int value) { this.experienceBeforeDecision = value; }
    public void setKnownCellsBeforeDecision(int value) { this.knownCellsBeforeDecision = value; }
    public void setLastObservationRound(int value) { this.lastObservationRound = value; }
    public void setInventory(Map<ItemType, Integer> value) { this.inventory = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value); }
    public void setInsideHouseId(UUID value) { this.insideHouseId = value; }
    public int itemCount(ItemType type) { return this.inventory.getOrDefault(type, 0); }
    public void addItem(ItemType type, int quantity) { this.inventory.merge(type, Math.max(0, quantity), Integer::sum); }
    public boolean removeItem(ItemType type, int quantity) { int current = this.itemCount(type); if (quantity < 0 || current < quantity) { return false; } this.inventory.put(type, current - quantity); return true; }
    public void heal(int amount) { this.setHealthPoints(this.healthPoints + amount); }
    public void hurt(int amount) { this.setHealthPoints(this.healthPoints - amount); }
    public int gainExperience(int amount, int maxHealthGainPerLevel, int attackDamageGainPerLevel) {
        int previous = this.level;
        this.setExperiencePoints(this.experiencePoints + amount);
        int levelsGained = this.level - previous;
        if (levelsGained > 0) {
            this.setMaxHealthPoints(this.maxHealthPoints + levelsGained * maxHealthGainPerLevel);
            this.setAttackDamage(this.attackDamage + levelsGained * attackDamageGainPerLevel);
            this.setHealthPoints(this.maxHealthPoints);
        }
        return previous;
    }

    public void beginDecision(String situationKey, ActionType actionType) {
        this.pendingSituationKey = situationKey;
        this.pendingDecisionAction = actionType;
        this.healthBeforeDecision = this.healthPoints;
        this.experienceBeforeDecision = this.experiencePoints;
        this.knownCellsBeforeDecision = this.mentalMap.size();
    }

    public ActionExperience learnFromDecision(long sequenceNumber, double reward, boolean rejected, double learningRate, int historyLimit) {
        int healthDelta = this.healthPoints - this.healthBeforeDecision;
        int experienceDelta = this.experiencePoints - this.experienceBeforeDecision;
        int knownCellsDelta = this.mentalMap.size() - this.knownCellsBeforeDecision;
        ActionExperience experience = new ActionExperience(sequenceNumber, this.pendingDecisionAction, this.pendingSituationKey, reward, healthDelta, experienceDelta, knownCellsDelta, rejected, this.alive);
        this.actionHistory.add(experience);
        while (this.actionHistory.size() > historyLimit) { this.actionHistory.remove(0); }
        String knowledgeKey = this.pendingSituationKey + "|" + this.pendingDecisionAction.name();
        this.learnedActions.computeIfAbsent(knowledgeKey, ignored -> new ActionKnowledge()).learn(reward, learningRate);
        this.pendingSituationKey = null;
        this.pendingDecisionAction = null;
        return experience;
    }

    public void remember(Cell cell, boolean visited, long sequence) {
        this.remember(cell, visited, sequence, List.of());
    }

    public void remember(Cell cell, boolean visited, long sequence, List<RememberedPeonObservation> rememberedPeons) {
        this.remember(cell, visited, sequence, rememberedPeons, List.of());
    }

    public void remember(Cell cell, boolean visited, long sequence, List<RememberedPeonObservation> rememberedPeons, List<RememberedGraveObservation> rememberedGraves) {
        String key = cell.getCoordinate().q() + ":" + cell.getCoordinate().r();
        RememberedCell remembered = this.mentalMap.get(key);
        if (remembered == null) {
            this.mentalMap.put(key, new RememberedCell(cell, visited, sequence, rememberedPeons, rememberedGraves));
        } else {
            remembered.refresh(cell, visited, sequence, rememberedPeons, rememberedGraves);
        }
    }
}
