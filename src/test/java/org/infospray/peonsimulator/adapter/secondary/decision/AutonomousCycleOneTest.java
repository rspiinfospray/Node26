package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.PeonPersonality;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AutonomousCycleOneTest {
    private final UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID teamId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID otherId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties());

    @Test
    void shouldNotClassifyAnAvailableMealAsLethalBecauseOfAnEnemy() {
        PeonDecisionContext.ObservedPeon enemy = new PeonDecisionContext.ObservedPeon(this.otherId, this.otherId, 200, 10);
        PeonDecision decision = this.provider.decide(this.context(40, 1, 0, List.of(enemy)));

        assertThat(decision.action().type()).isEqualTo(ActionType.MANGER);
        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.MANGER).allSatisfy(candidate -> assertThat(candidate.admissible()).isTrue());
    }

    @Test
    void shouldNotRewardUnknownFoodAsIfObservationWereARescue() {
        PeonDecision decision = this.provider.decide(this.context(45, 0, 0, List.of()));

        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.VOIR).hasSize(1).allSatisfy(candidate -> {
            assertThat(candidate.factors()).doesNotContainKey("recherche d'une issue de survie inconnue");
            assertThat(candidate.score()).isLessThan(75);
        });
    }

    @Test
    void shouldPreserveTheDonorsReserveBeforeOfferingAGift() {
        PeonDecisionContext.ObservedPeon ally = new PeonDecisionContext.ObservedPeon(this.otherId, this.teamId, 20, 100, 1, 0, 0);
        PeonDecision unsafe = this.provider.decide(this.context(80, 0, 40, List.of(ally)));
        PeonDecision safe = this.provider.decide(this.context(80, 0, 100, List.of(ally)));

        assertThat(unsafe.candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.DONNER_NOURRITURE);
        assertThat(safe.candidates()).extracting(PeonDecision.CandidateScore::actionType).contains(ActionType.DONNER_NOURRITURE);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"80,69,false", "80,70,true", "60,84,false", "60,85,true", "80,5,false"})
    void shouldApplyTheExistingReserveBudgetToTheFullGift(int health, int reserve, boolean giftAvailable) {
        PeonDecisionContext.ObservedPeon ally = new PeonDecisionContext.ObservedPeon(this.otherId, this.teamId, 20, 100, 1, 0, 0);
        PeonDecision decision = this.provider.decide(this.context(health, 0, reserve, List.of(ally)));

        assertThat(decision.candidates().stream().anyMatch(candidate -> candidate.actionType() == ActionType.DONNER_NOURRITURE)).isEqualTo(giftAvailable);
    }

    private PeonDecisionContext context(int health, int food, int reserve, List<PeonDecisionContext.ObservedPeon> peons) {
        return new PeonDecisionContext(this.actorId, this.teamId, health, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(10, 10, 50, 100), Map.of(), Map.of(), peons, food, List.of(), 10, 7, TerrainType.PLAIN, 0, 0, reserve, 0, false, null, List.of(), 0, 0, 0, List.of(), null, null, List.of(), List.of(), false, false, 1, null);
    }
}
