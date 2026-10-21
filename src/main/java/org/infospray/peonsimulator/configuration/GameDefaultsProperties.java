package org.infospray.peonsimulator.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "simulator.game")
public class GameDefaultsProperties {
    private int worldWidth = 50;
    private int worldHeight = 50;
    private double rockPercentage = 20;
    private double treePercentage = 15;
    private double foodCellPercentage = 15;
    private int minFoodPerCell = 1;
    private int maxFoodPerCell = 3;
    private int sheepCount = 10;
    private int maxRounds = 100;
    private int initialHealthPoints = 100;
    private int maxHealthPoints = 100;
    private int initialExperiencePoints;
    private int experiencePerLevel = 100;
    private int hungerHealthLossPerTurn = 5;
    private int eatHealthGain = 20;
    private int eatExperienceGain = 20;
    private int sheepHealthGain = 60;
    private int sheepExperienceGain = 30;
    private int sheepMovementPercentage = 50;
    private int moveExperienceGain = 5;
    private int moveHealthCost = 5;
    private int attackExperienceGain = 40;
    private int lethalAttackExperienceGain = 60;
    private int attackDamage = 20;
    private int attackDamageGainPerLevel = 10;
    private int maxHealthGainPerLevel = 10;
    private int eatHealthThreshold = 90;
    private int attackMinimumHealth = 35;
    private int enemyFleeHealthThreshold = 45;
    private int enemyPursuitHealthThreshold = 70;
    private int seekFoodHealthThreshold = 70;
    private int communicationFrequency = 4;
    private double learningRate = 0.20;
    private int explorationPercentage = 10;
    private int explorationMaximumScoreGap = 20;
    private int criticalHealthExplorationThreshold = 20;
    private int actionHistoryLimit = 100;
    private int treeHealthPoints = 100;
    private int woodPerChop = 25;
    private int woodRequiredForHouse = 200;
    private int chopWoodExperienceGain = 20;
    private int buildHouseExperienceGain = 70;
    private int lootExperienceGain = 10;
    private int houseHungerPercentage = 50;
    private int defaultZoomPercent = 170;
    private int woodRequiredForField = 50;
    private int woodRequiredForGranary = 100;
    private int woodRequiredForBridge = 50;
    private int buildBridgeExperienceGain = 50;
    private int fieldGrowthRounds = 8;
    private int fieldWheatYield = 5;
    private int granaryCapacity = 50;
    private int wheatHealthGain = 40;
    private int wheatExperienceGain = 15;
    private int harvestExperienceGain = 20;
    private int buildFieldExperienceGain = 35;
    private int buildGranaryExperienceGain = 50;
    private int constructionInfluenceRadius = 2;

    public int getWorldWidth() { return this.worldWidth; }
    public void setWorldWidth(int value) { this.worldWidth = value; }
    public int getWorldHeight() { return this.worldHeight; }
    public void setWorldHeight(int value) { this.worldHeight = value; }
    public double getRockPercentage() { return this.rockPercentage; }
    public void setRockPercentage(double value) { this.rockPercentage = value; }
    public double getTreePercentage() { return this.treePercentage; }
    public void setTreePercentage(double value) { this.treePercentage = value; }
    public double getFoodCellPercentage() { return this.foodCellPercentage; }
    public void setFoodCellPercentage(double value) { this.foodCellPercentage = value; }
    public int getMinFoodPerCell() { return this.minFoodPerCell; }
    public void setMinFoodPerCell(int value) { this.minFoodPerCell = value; }
    public int getMaxFoodPerCell() { return this.maxFoodPerCell; }
    public void setMaxFoodPerCell(int value) { this.maxFoodPerCell = value; }
    public int getSheepCount() { return this.sheepCount; }
    public void setSheepCount(int value) { this.sheepCount = value; }
    public int getMaxRounds() { return this.maxRounds; }
    public void setMaxRounds(int value) { this.maxRounds = value; }
    public int getInitialHealthPoints() { return this.initialHealthPoints; }
    public void setInitialHealthPoints(int value) { this.initialHealthPoints = value; }
    public int getMaxHealthPoints() { return this.maxHealthPoints; }
    public void setMaxHealthPoints(int value) { this.maxHealthPoints = value; }
    public int getInitialExperiencePoints() { return this.initialExperiencePoints; }
    public void setInitialExperiencePoints(int value) { this.initialExperiencePoints = value; }
    public int getExperiencePerLevel() { return this.experiencePerLevel; }
    public void setExperiencePerLevel(int value) { this.experiencePerLevel = value; }
    public int getHungerHealthLossPerTurn() { return this.hungerHealthLossPerTurn; }
    public void setHungerHealthLossPerTurn(int value) { this.hungerHealthLossPerTurn = value; }
    public int getEatHealthGain() { return this.eatHealthGain; }
    public void setEatHealthGain(int value) { this.eatHealthGain = value; }
    public int getEatExperienceGain() { return this.eatExperienceGain; }
    public void setEatExperienceGain(int value) { this.eatExperienceGain = value; }
    public int getSheepHealthGain() { return this.sheepHealthGain; }
    public void setSheepHealthGain(int value) { this.sheepHealthGain = value; }
    public int getSheepExperienceGain() { return this.sheepExperienceGain; }
    public void setSheepExperienceGain(int value) { this.sheepExperienceGain = value; }
    public int getSheepMovementPercentage() { return this.sheepMovementPercentage; }
    public void setSheepMovementPercentage(int value) { this.sheepMovementPercentage = value; }
    public int getMoveExperienceGain() { return this.moveExperienceGain; }
    public void setMoveExperienceGain(int value) { this.moveExperienceGain = value; }
    public int getMoveHealthCost() { return this.moveHealthCost; }
    public void setMoveHealthCost(int value) { this.moveHealthCost = value; }
    public int getAttackExperienceGain() { return this.attackExperienceGain; }
    public void setAttackExperienceGain(int value) { this.attackExperienceGain = value; }
    public int getLethalAttackExperienceGain() { return this.lethalAttackExperienceGain; }
    public void setLethalAttackExperienceGain(int value) { this.lethalAttackExperienceGain = value; }
    public int getAttackDamage() { return this.attackDamage; }
    public void setAttackDamage(int value) { this.attackDamage = value; }
    public int getAttackDamageGainPerLevel() { return this.attackDamageGainPerLevel; }
    public void setAttackDamageGainPerLevel(int value) { this.attackDamageGainPerLevel = value; }
    public int getMaxHealthGainPerLevel() { return this.maxHealthGainPerLevel; }
    public void setMaxHealthGainPerLevel(int value) { this.maxHealthGainPerLevel = value; }
    public int getEatHealthThreshold() { return this.eatHealthThreshold; }
    public void setEatHealthThreshold(int value) { this.eatHealthThreshold = value; }
    public int getAttackMinimumHealth() { return this.attackMinimumHealth; }
    public void setAttackMinimumHealth(int value) { this.attackMinimumHealth = value; }
    public int getEnemyFleeHealthThreshold() { return this.enemyFleeHealthThreshold; }
    public void setEnemyFleeHealthThreshold(int value) { this.enemyFleeHealthThreshold = value; }
    public int getEnemyPursuitHealthThreshold() { return this.enemyPursuitHealthThreshold; }
    public void setEnemyPursuitHealthThreshold(int value) { this.enemyPursuitHealthThreshold = value; }
    public int getSeekFoodHealthThreshold() { return this.seekFoodHealthThreshold; }
    public void setSeekFoodHealthThreshold(int value) { this.seekFoodHealthThreshold = value; }
    public int getCommunicationFrequency() { return this.communicationFrequency; }
    public void setCommunicationFrequency(int value) { this.communicationFrequency = value; }
    public double getLearningRate() { return this.learningRate; }
    public void setLearningRate(double value) { this.learningRate = value; }
    public int getExplorationPercentage() { return this.explorationPercentage; }
    public void setExplorationPercentage(int value) { this.explorationPercentage = value; }
    public int getExplorationMaximumScoreGap() { return this.explorationMaximumScoreGap; }
    public void setExplorationMaximumScoreGap(int value) { this.explorationMaximumScoreGap = value; }
    public int getCriticalHealthExplorationThreshold() { return this.criticalHealthExplorationThreshold; }
    public void setCriticalHealthExplorationThreshold(int value) { this.criticalHealthExplorationThreshold = value; }
    public int getActionHistoryLimit() { return this.actionHistoryLimit; }
    public void setActionHistoryLimit(int value) { this.actionHistoryLimit = value; }
    public int getDefaultZoomPercent() { return this.defaultZoomPercent; }
    public void setDefaultZoomPercent(int value) { this.defaultZoomPercent = value; }
    public int getTreeHealthPoints() { return this.treeHealthPoints; }
    public void setTreeHealthPoints(int value) { this.treeHealthPoints = value; }
    public int getWoodPerChop() { return this.woodPerChop; }
    public void setWoodPerChop(int value) { this.woodPerChop = value; }
    public int getWoodRequiredForHouse() { return this.woodRequiredForHouse; }
    public void setWoodRequiredForHouse(int value) { this.woodRequiredForHouse = value; }
    public int getChopWoodExperienceGain() { return this.chopWoodExperienceGain; }
    public void setChopWoodExperienceGain(int value) { this.chopWoodExperienceGain = value; }
    public int getBuildHouseExperienceGain() { return this.buildHouseExperienceGain; }
    public void setBuildHouseExperienceGain(int value) { this.buildHouseExperienceGain = value; }
    public int getLootExperienceGain() { return this.lootExperienceGain; }
    public void setLootExperienceGain(int value) { this.lootExperienceGain = value; }
    public int getHouseHungerPercentage() { return this.houseHungerPercentage; }
    public void setHouseHungerPercentage(int value) { this.houseHungerPercentage = value; }
    public int getWoodRequiredForField() { return this.woodRequiredForField; }
    public void setWoodRequiredForField(int value) { this.woodRequiredForField = value; }
    public int getWoodRequiredForGranary() { return this.woodRequiredForGranary; }
    public void setWoodRequiredForGranary(int value) { this.woodRequiredForGranary = value; }
    public int getWoodRequiredForBridge() { return this.woodRequiredForBridge; }
    public void setWoodRequiredForBridge(int value) { this.woodRequiredForBridge = value; }
    public int getBuildBridgeExperienceGain() { return this.buildBridgeExperienceGain; }
    public void setBuildBridgeExperienceGain(int value) { this.buildBridgeExperienceGain = value; }
    public int getFieldGrowthRounds() { return this.fieldGrowthRounds; }
    public void setFieldGrowthRounds(int value) { this.fieldGrowthRounds = value; }
    public int getFieldWheatYield() { return this.fieldWheatYield; }
    public void setFieldWheatYield(int value) { this.fieldWheatYield = value; }
    public int getGranaryCapacity() { return this.granaryCapacity; }
    public void setGranaryCapacity(int value) { this.granaryCapacity = value; }
    public int getWheatHealthGain() { return this.wheatHealthGain; }
    public void setWheatHealthGain(int value) { this.wheatHealthGain = value; }
    public int getWheatExperienceGain() { return this.wheatExperienceGain; }
    public void setWheatExperienceGain(int value) { this.wheatExperienceGain = value; }
    public int getHarvestExperienceGain() { return this.harvestExperienceGain; }
    public void setHarvestExperienceGain(int value) { this.harvestExperienceGain = value; }
    public int getBuildFieldExperienceGain() { return this.buildFieldExperienceGain; }
    public void setBuildFieldExperienceGain(int value) { this.buildFieldExperienceGain = value; }
    public int getBuildGranaryExperienceGain() { return this.buildGranaryExperienceGain; }
    public void setBuildGranaryExperienceGain(int value) { this.buildGranaryExperienceGain = value; }
    public int getConstructionInfluenceRadius() { return this.constructionInfluenceRadius; }
    public void setConstructionInfluenceRadius(int value) { this.constructionInfluenceRadius = Math.max(0, value); }
}
