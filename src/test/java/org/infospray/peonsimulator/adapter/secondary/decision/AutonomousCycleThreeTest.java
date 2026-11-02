package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AutonomousCycleThreeTest {
    private final UUID teamId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final HexCoordinate position = new HexCoordinate(2, 2);
    private final HexCoordinate neighbor = new HexCoordinate(3, 2);
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties());

    @Test
    void shouldExcludeLootAndChoppingWhenWoodInventoryIsFull() {
        PeonDecision decision = this.provider.decide(this.context(100, 300, null, 0));
        assertThat(decision.candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.PILLER, ActionType.COUPER_DU_BOIS);
        assertThat(this.provider.decide(this.context(100, 299, null, 0)).candidates()).extracting(PeonDecision.CandidateScore::actionType).contains(ActionType.PILLER, ActionType.COUPER_DU_BOIS);
    }

    @Test
    void shouldValueOnlyTheLootThatFitsInInventory() {
        PeonDecision decision = this.provider.decide(this.context(100, 299, null, 0));
        assertThat(this.candidate(decision, ActionType.PILLER).factors().get("bois récupérable")).isEqualTo(0.28);
    }

    @Test
    void shouldStopAwardingCollectionProjectBonusOnceWoodBudgetIsReached() {
        LongTermStrategy strategy = this.strategy(StrategyType.CONSTRUIRE_UN_FOYER, StrategicPhase.RASSEMBLER_DU_BOIS);
        PeonDecision before = this.provider.decide(this.context(100, 199, strategy, 0));
        PeonDecision after = this.provider.decide(this.context(100, 200, strategy, 0));
        for (ActionType type : List.of(ActionType.PILLER, ActionType.COUPER_DU_BOIS)) {
            assertThat(this.candidate(before, type).factors().keySet()).anyMatch(key -> key.startsWith("projet à long terme"));
            assertThat(this.candidate(after, type).factors().keySet()).noneMatch(key -> key.startsWith("projet à long terme"));
        }
    }

    @Test
    void shouldNotRewardObservationForApproachingAThreat() {
        LongTermStrategy strategy = this.strategy(StrategyType.PROTEGER_L_EQUIPE, StrategicPhase.REJOINDRE_LA_MENACE);
        PeonDecision decision = this.provider.decide(this.context(100, 0, strategy, 0));
        assertThat(this.candidate(decision, ActionType.VOIR).factors().keySet()).noneMatch(key -> key.startsWith("projet à long terme"));
    }

    @Test
    void shouldNotMoveTowardAnInactiveAgriculturalProject() {
        for (StrategyStatus status : List.of(StrategyStatus.ACCOMPLIE, StrategyStatus.ABANDONNEE)) {
            LongTermStrategy strategy = this.strategy(StrategyType.AUTONOMIE_ALIMENTAIRE, StrategicPhase.CONSTRUIRE_UN_GRENIER);
            strategy.setTargetArea(this.position);
            strategy.setStatus(status);
            PeonDecision decision = this.provider.decide(this.context(100, 0, strategy, 0));
            assertThat(this.candidate(decision, ActionType.SE_DEPLACER).factors()).doesNotContainKey("progression vers l'exploitation");
        }
    }

    @Test
    void shouldSearchUnvisitedCellInsteadOfStarvingPassively() {
        PeonDecisionContext context = this.context(40, 0, null, 0);
        ActionKnowledge knowledge = new ActionKnowledge();
        knowledge.setExpectedReward(10000);
        context.learnedActions().put(this.provider.decide(context).situationKey() + "|NE_RIEN_FAIRE", knowledge);
        assertThat(this.provider.decide(context).action().type()).isEqualTo(ActionType.SE_DEPLACER);
    }

    @Test
    void shouldNotUseFoodSearchAsAnExcuseForLethalMovement() {
        assertThat(this.provider.decide(this.context(5, 0, null, 0)).action().type()).isNotEqualTo(ActionType.SE_DEPLACER);
    }

    @Test
    void shouldKeepImmediateFoodAheadOfEmergencySearch() {
        assertThat(this.provider.decide(this.context(40, 0, null, 10)).action().type()).isEqualTo(ActionType.MANGER);
    }

    @Test
    void shouldNotSearchAVisitedOrEnemyOccupiedCell() {
        PeonDecisionContext context = this.context(40, 0, null, 0);
        RememberedCell memory = new RememberedCell(new Cell(this.neighbor, TerrainType.PLAIN, 0), true, 9);
        context.mentalMap().put("3:2", memory);
        assertThat(this.provider.decide(context).action().type()).isNotEqualTo(ActionType.SE_DEPLACER);
        memory.setVisited(false);
        memory.setRememberedPeons(List.of(new RememberedPeonObservation(UUID.fromString("00000000-0000-0000-0000-000000000003"), UUID.fromString("00000000-0000-0000-0000-000000000004"), PeonRelation.ENEMY, this.neighbor, 100, 1, 9)));
        assertThat(this.provider.decide(context).action().type()).isNotEqualTo(ActionType.SE_DEPLACER);
    }

    private PeonDecision.CandidateScore candidate(PeonDecision decision, ActionType type) {
        return decision.candidates().stream().filter(candidate -> candidate.actionType() == type).findFirst().orElseThrow();
    }

    private LongTermStrategy strategy(StrategyType type, StrategicPhase phase) {
        LongTermStrategy strategy = new LongTermStrategy(type, 1, this.teamId);
        strategy.setPhase(phase);
        return strategy;
    }

    private PeonDecisionContext context(int health, int wood, LongTermStrategy strategy, int food) {
        return new PeonDecisionContext(this.teamId, this.teamId, health, 100, 0, 1,
                this.position, false, 10, 42, new PeonPersonality(50, 50, 50, 50),
                new HashMap<>(), new HashMap<>(), List.of(), food, List.of(this.neighbor), 10, 0,
                TerrainType.TREE, 100, wood, 0, 150, false, null, List.of(), 0, 0,
                0, List.of(), null, null, List.of(), List.of(), false, false, 1, strategy,
                100, 5);
    }
}
