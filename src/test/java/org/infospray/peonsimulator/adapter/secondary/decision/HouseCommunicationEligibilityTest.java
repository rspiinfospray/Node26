package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class HouseCommunicationEligibilityTest {
    private final UUID actor = new UUID(0, 1);
    private final UUID ally = new UUID(0, 2);
    private final UUID team = new UUID(0, 3);
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties());

    @Test
    void shouldInviteOnlyAnAllyStillOutside() {
        assertThat(this.provider.decide(this.context(true, false, 0)).candidates()).anyMatch(candidate -> candidate.factors().containsKey("invitation d'un allié au foyer"));
        assertThat(this.provider.decide(this.context(true, true, 0)).candidates()).noneMatch(candidate -> candidate.actionType() == ActionType.COMMUNIQUER);
        assertThat(this.provider.decide(this.context(true, true, 1)).candidates()).filteredOn(candidate -> candidate.actionType() == ActionType.COMMUNIQUER).hasSize(1).allSatisfy(candidate -> assertThat(candidate.factors()).doesNotContainKey("invitation d'un allié au foyer"));
    }

    @Test
    void shouldNotRequestProtectionAgainWhenAlreadyInside() {
        assertThat(this.provider.decide(this.context(false, true, 0)).candidates()).noneMatch(candidate -> candidate.actionType() == ActionType.COMMUNIQUER);
    }

    private PeonDecisionContext context(boolean host, boolean allyInside, int usefulInformation) {
        HexCoordinate position = new HexCoordinate(2, 2);
        PeonDecisionContext.ObservedHouse house = new PeonDecisionContext.ObservedHouse(new UUID(0, 4), host ? this.actor : this.ally, this.team, position, true, true);
        PeonDecisionContext.ObservedPeon neighbor = new PeonDecisionContext.ObservedPeon(this.ally, this.team, 100, 100, 1, 0, 0, allyInside);
        return new PeonDecisionContext(this.actor, this.team, 20, 100, 0, 1, position, false, 10, 42, new PeonPersonality(50, 50, 50, 100), Map.of(), Map.of(), List.of(neighbor), 0, List.of(), 10, 9, TerrainType.PLAIN, 0, 0, 0, false, house, List.of(house), usefulInformation, 0);
    }
}
