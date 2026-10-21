package org.infospray.peonsimulator.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class LongTermStrategy {
    private StrategyType type;
    private StrategyStatus status = StrategyStatus.EN_COURS;
    private StrategicPhase phase;
    private int startedAtRound;
    private int lastEvaluatedAtRound;
    private int commitmentUntilRound;
    private int horizonRounds = 40;
    private int tacticalUntilRound;
    private UUID targetTeamId;
    private UUID targetHouseId;
    private UUID targetGranaryId;
    private HexCoordinate targetArea;
    private Map<String, Integer> objectives = new LinkedHashMap<>();
    private double progress;
    private double expectedValue;
    private String tacticalObjective;
    private String nextPriority;
    private int lastProgressAtRound;
    private int stagnantEvaluations;

    public LongTermStrategy() {
    }

    public LongTermStrategy(StrategyType type, int currentRound, UUID targetTeamId) {
        this.type = type;
        this.startedAtRound = currentRound;
        this.lastEvaluatedAtRound = currentRound;
        this.lastProgressAtRound = currentRound;
        this.commitmentUntilRound = currentRound + 10;
        this.targetTeamId = targetTeamId;
    }

    public StrategyType getType() { return this.type; }
    public StrategyStatus getStatus() { return this.status; }
    public StrategicPhase getPhase() { return this.phase; }
    public int getStartedAtRound() { return this.startedAtRound; }
    public int getLastEvaluatedAtRound() { return this.lastEvaluatedAtRound; }
    public int getCommitmentUntilRound() { return this.commitmentUntilRound; }
    public int getHorizonRounds() { return this.horizonRounds; }
    public int getTacticalUntilRound() { return this.tacticalUntilRound; }
    public UUID getTargetTeamId() { return this.targetTeamId; }
    public UUID getTargetHouseId() { return this.targetHouseId; }
    public UUID getTargetGranaryId() { return this.targetGranaryId; }
    public HexCoordinate getTargetArea() { return this.targetArea; }
    public Map<String, Integer> getObjectives() { return this.objectives; }
    public double getProgress() { return this.progress; }
    public double getExpectedValue() { return this.expectedValue; }
    public String getTacticalObjective() { return this.tacticalObjective; }
    public String getNextPriority() { return this.nextPriority; }
    public int getLastProgressAtRound() { return this.lastProgressAtRound; }
    public int getStagnantEvaluations() { return this.stagnantEvaluations; }
    public void setType(StrategyType value) { this.type = value; }
    public void setStatus(StrategyStatus value) { this.status = value; }
    public void setPhase(StrategicPhase value) { this.phase = value; }
    public void setStartedAtRound(int value) { this.startedAtRound = value; }
    public void setLastEvaluatedAtRound(int value) { this.lastEvaluatedAtRound = value; }
    public void setCommitmentUntilRound(int value) { this.commitmentUntilRound = value; }
    public void setHorizonRounds(int value) { this.horizonRounds = value; }
    public void setTacticalUntilRound(int value) { this.tacticalUntilRound = value; }
    public void setTargetTeamId(UUID value) { this.targetTeamId = value; }
    public void setTargetHouseId(UUID value) { this.targetHouseId = value; }
    public void setTargetGranaryId(UUID value) { this.targetGranaryId = value; }
    public void setTargetArea(HexCoordinate value) { this.targetArea = value; }
    public void setObjectives(Map<String, Integer> value) { this.objectives = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value); }
    public void setProgress(double value) { this.progress = Math.max(0, Math.min(100, value)); }
    public void setExpectedValue(double value) { this.expectedValue = value; }
    public void setTacticalObjective(String value) { this.tacticalObjective = value; }
    public void setNextPriority(String value) { this.nextPriority = value; }
    public void setLastProgressAtRound(int value) { this.lastProgressAtRound = value; }
    public void setStagnantEvaluations(int value) { this.stagnantEvaluations = Math.max(0, value); }
}
