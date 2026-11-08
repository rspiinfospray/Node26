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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportCycleOneRegressionTest {
    private final UUID actor = new UUID(0, 1);
    private final UUID team = new UUID(0, 2);
    private final HexCoordinate destination = new HexCoordinate(3, 2);
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties());

    @ParameterizedTest
    @ValueSource(ints = {15, 20, 35, 45})
    void shouldSearchInsteadOfWaitingForCertainStarvation(int health) {
        PeonDecision decision = this.provider.decide(this.context(health, List.of()));
        assertThat(decision.action().type()).isEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.action().destination()).isEqualTo(this.destination);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10})
    void shouldStillRejectImmediatelyLethalSearch(int health) {
        PeonDecision decision = this.provider.decide(this.context(health, List.of()));
        assertThat(decision.action().type()).isNotEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).allSatisfy(candidate -> assertThat(candidate.exclusionReason()).isEqualTo("issue prévisible létale"));
    }

    @Test
    void shouldNotOfferAnAttackBelowTheConfiguredMinimum() {
        var enemy = new PeonDecisionContext.ObservedPeon(new UUID(0, 3), new UUID(0, 4), 100, 1);
        assertThat(this.provider.decide(this.context(25, List.of(enemy))).candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.ATTAQUER);
        assertThat(this.provider.decide(this.context(35, List.of(enemy))).candidates()).extracting(PeonDecision.CandidateScore::actionType).contains(ActionType.ATTAQUER);
    }

    @Test
    void shouldExplainTheFoodPriorityFilterRatherThanAnUnappliedThreshold() {
        PeonDecision decision = this.provider.decide(this.context(45, List.of()));
        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.NE_RIEN_FAIRE).allSatisfy(candidate -> assertThat(candidate.exclusionReason()).isEqualTo("priorité de survie"));
    }

    @Test
    void shouldNotClaimAnUnshelteredActorIsSheltered() {
        PeonDecisionContext context = this.context(45, List.of());
        context.mentalMap().put("3:2", new RememberedCell(new Cell(this.destination, TerrainType.PLAIN, 10), false, 1));
        assertThat(this.provider.decide(context).candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.NE_RIEN_FAIRE).allSatisfy(candidate -> assertThat(candidate.factors()).containsKey("attente dangereuse sans abri").doesNotContainKey("attente dangereuse malgré l'abri"));
    }

    private PeonDecisionContext context(int health, List<PeonDecisionContext.ObservedPeon> peons) {
        return new PeonDecisionContext(this.actor, this.team, health, 100, 0, 1,
                new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 0, 50, 50),
                new HashMap<>(), new HashMap<>(), peons, 0, List.of(this.destination), 10, 7);
    }
}
