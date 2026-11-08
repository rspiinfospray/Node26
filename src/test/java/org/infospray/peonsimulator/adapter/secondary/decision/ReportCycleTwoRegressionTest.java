package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportCycleTwoRegressionTest {
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties());
    private final HexCoordinate origin = new HexCoordinate(2, 2);
    private final HexCoordinate step = new HexCoordinate(3, 2);

    @Test
    void shouldAcceptTwoStepFoodRouteWithEightHealthRemaining() {
        Map<String, RememberedCell> map = new HashMap<>();
        this.remember(map, this.step, 0);
        this.remember(map, new HexCoordinate(4, 2), 40);
        PeonDecision decision = this.provider.decide(this.context(28, map, List.of(this.step), null));
        assertThat(decision.action().type()).isEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).allSatisfy(candidate -> assertThat(candidate.admissible()).isTrue());
    }

    @Test
    void shouldUseAnotherFoodSourceWhenClosestHasNoProgressingNeighbor() {
        Map<String, RememberedCell> map = new HashMap<>();
        this.remember(map, new HexCoordinate(1, 2), 40);
        this.remember(map, new HexCoordinate(4, 2), 40);
        this.remember(map, this.step, 0);
        PeonDecision decision = this.provider.decide(this.context(45, map, List.of(this.step), null));
        assertThat(decision.action().destination()).isEqualTo(this.step);
        assertThat(this.move(decision).factors()).containsKey("route alimentaire prioritaire · baies");
        assertThat(this.move(decision).factors().get("route alimentaire prioritaire · baies")).isPositive();
    }

    @Test
    void shouldKeepFoodUrgencyWhenNoResourceIsKnown() {
        PeonDecision decision = this.provider.decide(this.context(45, Map.of(), List.of(), null));
        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.NE_RIEN_FAIRE).allSatisfy(candidate -> assertThat(candidate.factors()).containsKey("attente dangereuse sans abri"));
    }

    @Test
    void shouldSuspendStrategicMovementBonusDuringFoodEmergency() {
        LongTermStrategy strategy = new LongTermStrategy(StrategyType.PROTEGER_L_EQUIPE, 1, new UUID(0, 2));
        strategy.setPhase(StrategicPhase.REJOINDRE_LA_MENACE);
        strategy.setTargetArea(new HexCoordinate(6, 2));
        strategy.setStagnantEvaluations(3);
        assertThat(this.move(this.provider.decide(this.context(45, Map.of(), List.of(this.step), strategy))).factors()).doesNotContainKey("progression vers la cible stratégique");
        assertThat(this.move(this.provider.decide(this.context(45, Map.of(), List.of(this.step), strategy))).factors().keySet()).noneMatch(key -> key.startsWith("projet à long terme"));
        assertThat(this.move(this.provider.decide(this.context(100, Map.of(), List.of(this.step), strategy))).factors()).containsKey("progression vers la cible stratégique");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 6, 10})
    void shouldKeepImmediatelyLethalMovementExcluded(int health) {
        PeonDecision decision = this.provider.decide(this.context(health, Map.of(), List.of(this.step), null));
        assertThat(this.move(decision).admissible()).isFalse();
        assertThat(this.move(decision).exclusionReason()).isEqualTo("issue prévisible létale");
    }

    private PeonDecision.CandidateScore move(PeonDecision decision) {
        return decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).findFirst().orElseThrow();
    }

    private void remember(Map<String, RememberedCell> map, HexCoordinate position, int food) {
        map.put(position.q() + ":" + position.r(), new RememberedCell(new Cell(position, TerrainType.PLAIN, food), true, 1));
    }

    private PeonDecisionContext context(int health, Map<String, RememberedCell> map, List<HexCoordinate> neighbors, LongTermStrategy strategy) {
        return new PeonDecisionContext(new UUID(0, 1), new UUID(0, 2), health, 100, 0, 1, this.origin, false, 10, 42, new PeonPersonality(50, 50, 50, 50), Map.of(), map, List.of(), 0, neighbors, 10, 9, TerrainType.PLAIN, 0, 0, 0, 0, false, null, List.of(), 0, 0, 0, List.of(), null, null, List.of(), List.of(), false, false, 1, strategy);
    }
}
