package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.ActionKnowledge;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.Cell;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.PeonPersonality;
import org.infospray.peonsimulator.domain.model.PeonRelation;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.RememberedPeonObservation;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedPeonDecisionProviderTest {
    private final GameDefaultsProperties defaults = this.defaults();
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(this.defaults);

    @Test
    void shouldEatWhenSurvivalUtilityIsHighest() {
        PeonDecision decision = this.provider.decide(this.context(35, false, Map.of(), Map.of(), List.of(), 2, List.of(new HexCoordinate(3, 2))));
        assertThat(decision.action().type()).isEqualTo(ActionType.MANGER);
        assertThat(decision.candidates()).extracting(PeonDecision.CandidateScore::actionType).contains(ActionType.MANGER, ActionType.SE_DEPLACER, ActionType.VOIR, ActionType.NE_RIEN_FAIRE);
    }

    @Test
    void shouldAttackWeakerEnemyWhenUtilityIsFavorable() {
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext.ObservedPeon enemy = new PeonDecisionContext.ObservedPeon(UUID.randomUUID(), UUID.randomUUID(), 35, 1);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 95, 100, 0, 2, new HexCoordinate(2, 2), false, 5, 42, new PeonPersonality(35, 85, 50, 50), Map.of(), Map.of(), List.of(enemy), 0, List.of(), 5, 4);
        assertThat(this.provider.decide(context).action().type()).isEqualTo(ActionType.ATTAQUER);
    }

    @Test
    void shouldValueTreeCoverAgainstDangerousRememberedEnemy() {
        HexCoordinate enemyPosition = new HexCoordinate(3, 2);
        Cell enemyCell = new Cell(enemyPosition, TerrainType.PLAIN, 0);
        RememberedPeonObservation enemy = new RememberedPeonObservation(UUID.randomUUID(), UUID.randomUUID(), PeonRelation.ENEMY, enemyPosition, 90, 3, 9);
        RememberedCell memory = new RememberedCell(enemyCell, false, 9, List.of(enemy));
        PeonDecision decision = this.provider.decide(this.context(40, true, Map.of(), Map.of("3:2", memory), List.of(), 0, List.of()));
        assertThat(decision.action().type()).isIn(ActionType.VOIR, ActionType.NE_RIEN_FAIRE);
        assertThat(decision.action().reason()).containsAnyOf("couvert", "camouflage");
    }

    @Test
    void shouldUseLearnedRewardInFutureDecision() {
        ActionKnowledge knowledge = new ActionKnowledge();
        knowledge.setExpectedReward(120);
        String situation = "PV_ELEVES:SEUL:NOURRITURE_INCONNUE:DECOUVERT";
        PeonDecision decision = this.provider.decide(this.context(80, false, Map.of(situation + "|NE_RIEN_FAIRE", knowledge), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2))));
        assertThat(decision.action().type()).isEqualTo(ActionType.NE_RIEN_FAIRE);
        assertThat(decision.candidates().getFirst().factors()).containsKey("expérience passée");
    }

    @Test
    void shouldExposeScoresAndSituationForExplanation() {
        PeonDecision decision = this.provider.decide(this.context(100, false, Map.of(), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2))));
        assertThat(decision.situationKey()).contains("PV_MAX", "SEUL");
        assertThat(decision.candidates()).isNotEmpty();
        assertThat(decision.selectedScore()).isEqualTo(decision.candidates().stream().filter(candidate -> candidate.actionType() == decision.action().type()).findFirst().orElseThrow().score());
    }

    @Test
    void shouldClassifyHealthFromMaximumHealthPercentage() {
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), UUID.randomUUID(), 60, 200, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 50), Map.of(), Map.of(), List.of(), 0, List.of(), 10, 9);
        assertThat(this.provider.decide(context).situationKey()).startsWith("PV_FAIBLES");
    }

    @Test
    void shouldNotObserveAgainWhenPerceptionWasRefreshedOnPreviousRound() {
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), UUID.randomUUID(), 80, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 90, 50), Map.of(), Map.of(), List.of(), 0, List.of(), 10, 9);
        PeonDecision decision = this.provider.decide(context);
        assertThat(decision.action().type()).isEqualTo(ActionType.NE_RIEN_FAIRE);
        assertThat(decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.VOIR).findFirst().orElseThrow().factors()).containsEntry("perception encore fraîche", -12.0);
    }

    private PeonDecisionContext context(int health, boolean concealed, Map<String, ActionKnowledge> learning, Map<String, RememberedCell> mentalMap, List<PeonDecisionContext.ObservedPeon> occupants, int food, List<HexCoordinate> neighbors) {
        return new PeonDecisionContext(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.randomUUID(), health, 100, 0, 1, new HexCoordinate(2, 2), concealed, 10, 42, new PeonPersonality(50, 50, 50, 50), learning, mentalMap, occupants, food, neighbors, 10, 9);
    }

    private GameDefaultsProperties defaults() {
        GameDefaultsProperties properties = new GameDefaultsProperties();
        properties.setExplorationPercentage(0);
        return properties;
    }
}
