package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.ActionKnowledge;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.PeonPersonality;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SurvivalPriorityTest {
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties());

    @Test
    void shouldEatBeforeRequestingFoodEvenWhenRequestHasAnOverwhelmingLearnedScore() {
        Map<String, ActionKnowledge> learning = new HashMap<>();
        PeonDecisionContext context = this.context(20, 1, learning);
        this.favor(context, ActionType.DEMANDER_NOURRITURE);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.MANGER);
        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.DEMANDER_NOURRITURE).allSatisfy(candidate -> {
            assertThat(candidate.admissible()).isFalse();
            assertThat(candidate.exclusionReason()).isEqualTo("priorité de survie");
        });
    }

    @Test
    void shouldAvoidAdditionalMovementDamageWhenNoActionCanSurviveHunger() {
        PeonDecisionContext context = this.context(4, 0, new HashMap<>());
        this.favor(context, ActionType.SE_DEPLACER);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isNotEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).allSatisfy(candidate -> {
            assertThat(candidate.admissible()).isFalse();
            assertThat(candidate.exclusionReason()).contains("létale");
        });
    }

    @Test
    void shouldKeepSafeMovementAvailableAtNormalHealth() {
        PeonDecisionContext context = this.context(100, 0, new HashMap<>());
        this.favor(context, ActionType.SE_DEPLACER);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.SE_DEPLACER);
    }

    private void favor(PeonDecisionContext context, ActionType type) {
        ActionKnowledge knowledge = new ActionKnowledge();
        knowledge.setExpectedReward(10000);
        context.learnedActions().put(this.provider.decide(context).situationKey() + "|" + type.name(), knowledge);
    }

    private PeonDecisionContext context(int health, int food, Map<String, ActionKnowledge> learning) {
        UUID teamId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        PeonDecisionContext.ObservedPeon ally = new PeonDecisionContext.ObservedPeon(UUID.fromString("00000000-0000-0000-0000-000000000003"), teamId, 100, 100, 1, 0, 50);
        return new PeonDecisionContext(UUID.fromString("00000000-0000-0000-0000-000000000001"), teamId, health, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 50), learning, Map.of(), food > 0 ? List.of(ally) : List.of(), food, List.of(new HexCoordinate(3, 2)), 10, 9);
    }
}
