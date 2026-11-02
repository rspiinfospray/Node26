package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class AutonomousCycleTwoTest {
    private final UUID teamId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties());

    @Test
    void shouldUseEffectiveWorldHungerInsteadOfDefaults() {
        PeonDecision decision = this.provider.decide(this.context(12, 1, 100, 20));
        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).allSatisfy(candidate -> {
            assertThat(candidate.admissible()).isFalse();
            assertThat(candidate.exclusionReason()).contains("létale");
            assertThat(candidate.factors().get("coût complet du tour en PV")).isLessThanOrEqualTo(-25);
        });
    }

    @Test
    void shouldOnlyBuildFieldsThatCanMatureByTheLastActionRound() {
        assertThat(this.provider.decide(this.context(100, 92, 100, 5)).candidates()).extracting(PeonDecision.CandidateScore::actionType).contains(ActionType.CONSTRUIRE_CHAMP);
        assertThat(this.provider.decide(this.context(100, 93, 100, 5)).candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.CONSTRUIRE_CHAMP);
        assertThat(this.provider.decide(this.context(100, 91, 120, 5)).candidates()).extracting(PeonDecision.CandidateScore::actionType).contains(ActionType.CONSTRUIRE_CHAMP);
    }

    @Test
    void shouldRejectFoodRoutesWithoutTwoHungerTurnsOfArrivalReserve() {
        assertThat(this.provider.decide(this.context(20, 1, 100, 5)).action().type()).isNotEqualTo(ActionType.SE_DEPLACER);
        assertThat(this.provider.decide(this.context(21, 1, 100, 5)).action().type()).isEqualTo(ActionType.SE_DEPLACER);
    }

    @Test
    void shouldMinimizeHealthLossWhenNoCandidateRespectsTheThreshold() {
        PeonDecisionContext context = this.context(20, 1, 100, 5);
        ActionKnowledge knowledge = new ActionKnowledge();
        knowledge.setExpectedReward(10000);
        context.learnedActions().put(this.provider.decide(context).situationKey() + "|SE_DEPLACER", knowledge);
        PeonDecision decision = this.provider.decide(context);
        assertThat(decision.action().type()).isNotEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).allSatisfy(candidate -> assertThat(candidate.admissible()).isFalse());
    }

    private PeonDecisionContext context(int health, int round, int endRound, int hunger) {
        HexCoordinate position = new HexCoordinate(2, 2);
        var granary = new PeonDecisionContext.ObservedGranary(this.teamId, this.teamId, position, 0);
        return new PeonDecisionContext(this.teamId, this.teamId, health, 100, 0, 1, position, false, 10, 42, new PeonPersonality(100, 100, 0, 0), new java.util.HashMap<>(), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2)), round, round - 2, TerrainType.PLAIN, 0, 50, 0, 0, false, null, List.of(), 0, 0, 0, List.of(new HexCoordinate(3, 2)), null, null, List.of(), List.of(granary), true, false, 1, null, endRound, hunger);
    }
}
