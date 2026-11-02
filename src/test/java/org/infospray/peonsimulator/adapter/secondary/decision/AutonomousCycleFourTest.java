package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class AutonomousCycleFourTest {
    private final UUID team = new UUID(0, 1);
    private final HexCoordinate position = new HexCoordinate(2, 2);
    private final HexCoordinate destination = new HexCoordinate(3, 2);
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties());

    @Test
    void shouldExcludeFoodInteractionsAcrossTheHouseDoor() {
        PeonDecision outside = this.provider.decide(this.context(50, true, 0));
        assertThat(outside.candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.DEMANDER_NOURRITURE);
        assertThat(this.provider.decide(this.context(50, false, 0)).candidates()).extracting(PeonDecision.CandidateScore::actionType).contains(ActionType.DEMANDER_NOURRITURE);
        assertThat(this.provider.decide(this.context(50, true, 100)).candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.DONNER_NOURRITURE, ActionType.TROQUER_NOURRITURE_CONTRE_BOIS);
        assertThat(this.provider.decide(this.context(50, false, 100)).candidates()).extracting(PeonDecision.CandidateScore::actionType).contains(ActionType.DONNER_NOURRITURE, ActionType.TROQUER_NOURRITURE_CONTRE_BOIS);
    }

    @Test
    void shouldReuseFailuresAcrossExactHealthAndStockValuesWithoutChangingHistory() {
        PeonDecisionContext context = this.context(40, false, 0);
        PeonDecisionContext previous = this.context(45, false, 1);
        ActionKnowledge failed = new ActionKnowledge();
        failed.setAttempts(2);
        failed.setFailures(2);
        failed.setExpectedReward(-20);
        String key = this.provider.decide(previous).situationKey() + "|VOIR";
        context.learnedActions().put(key, failed);
        PeonDecision.CandidateScore observation = this.provider.decide(context).candidates().stream().filter(candidate -> candidate.actionType() == ActionType.VOIR).findFirst().orElseThrow();
        assertThat(observation.factors()).containsEntry("expérience passée", -20.0).containsKey("échecs répétés");
        assertThat(context.learnedActions()).hasSize(1).containsEntry(key, failed);
        PeonDecisionContext differentHealthClass = this.context(90, false, 0);
        differentHealthClass.learnedActions().put(key, failed);
        assertThat(this.provider.decide(differentHealthClass).candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.VOIR).allSatisfy(candidate -> assertThat(candidate.factors()).doesNotContainKey("expérience passée"));
    }

    @Test
    void shouldUseTheLatestKnownEnemyPositionForMovementSafety() {
        PeonDecisionContext context = this.context(40, false, 0);
        context.peonsOnCurrentCell().clear();
        UUID enemyId = new UUID(0, 9);
        UUID enemyTeam = new UUID(0, 10);
        RememberedCell oldCell = new RememberedCell(new Cell(this.destination, TerrainType.PLAIN, 0), false, 1);
        oldCell.setRememberedPeons(List.of(new RememberedPeonObservation(enemyId, enemyTeam, PeonRelation.ENEMY, this.destination, 190, 10, 1)));
        context.mentalMap().put("3:2", oldCell);
        assertThat(this.provider.decide(context).candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).allSatisfy(candidate -> assertThat(candidate.admissible()).isFalse());
        HexCoordinate elsewhere = new HexCoordinate(8, 8);
        RememberedCell latest = new RememberedCell(new Cell(elsewhere, TerrainType.PLAIN, 0), false, 2);
        latest.setRememberedPeons(List.of(new RememberedPeonObservation(enemyId, enemyTeam, PeonRelation.ENEMY, elsewhere, 190, 10, 2)));
        context.mentalMap().put("8:8", latest);
        assertThat(this.provider.decide(context).action().type()).isEqualTo(ActionType.SE_DEPLACER);
    }

    private PeonDecisionContext context(int health, boolean inside, int food) {
        var house = new PeonDecisionContext.ObservedHouse(new UUID(0, 3), new UUID(0, 4), this.team, this.position, inside, true);
        var ally = new PeonDecisionContext.ObservedPeon(new UUID(0, 4), this.team, 50, 100, 1, 100, 10, false);
        return new PeonDecisionContext(new UUID(0, 2), this.team, health, 100, 0, 1,
                this.position, false, 10, 42, new PeonPersonality(50, 0, 50, 50),
                new HashMap<>(), new HashMap<>(), new java.util.ArrayList<>(List.of(ally)), 0, List.of(this.destination), 10, 0,
                TerrainType.PLAIN, 0, 0, food, 0, false, house, List.of(house), 0, 0,
                0, List.of(), null, null, List.of(), List.of(), false, false, 2, null,
                100, 5);
    }
}
