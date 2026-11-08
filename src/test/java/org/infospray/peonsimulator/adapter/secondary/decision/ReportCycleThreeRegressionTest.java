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

class ReportCycleThreeRegressionTest {
    private final GameDefaultsProperties defaults = new GameDefaultsProperties();
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(this.defaults);
    private final UUID actor = new UUID(0, 1);
    private final UUID team = new UUID(0, 2);
    private final HexCoordinate origin = new HexCoordinate(2, 2);
    private final HexCoordinate step = new HexCoordinate(3, 2);

    @Test
    void shouldSearchThroughVisitedCellTowardUnvisitedFrontier() {
        RememberedCell visited = new RememberedCell(new Cell(this.step, TerrainType.PLAIN, 0), true, 1);
        HexCoordinate frontier = new HexCoordinate(4, 2);
        RememberedCell unvisited = new RememberedCell(new Cell(frontier, TerrainType.PLAIN, 0), false, 1);
        PeonDecision decision = this.provider.decide(this.context(25, Map.of("3:2", visited, "4:2", unvisited), List.of(), List.of(), List.of()));
        assertThat(decision.action().destination()).isEqualTo(this.step);
        assertThat(this.move(decision).admissible()).isTrue();
    }

    @Test
    void shouldSearchWhenSheepArrivalReserveCannotBeMet() {
        PeonDecision decision = this.provider.decide(this.context(25, Map.of(), List.of(new HexCoordinate(4, 2)), List.of(), List.of()));
        assertThat(decision.action().destination()).isEqualTo(this.step);
        assertThat(this.move(decision).admissible()).isTrue();
        assertThat(this.move(decision).factors()).doesNotContainKey("route alimentaire prioritaire · mouton");
    }

    @Test
    void shouldNotOfferHarvestWhenAllKnownAlliedGranariesAreFull() {
        PeonDecisionContext.ObservedGranary full = new PeonDecisionContext.ObservedGranary(new UUID(0, 3), this.team, this.step, this.defaults.getGranaryCapacity());
        PeonDecisionContext.ObservedGranary available = new PeonDecisionContext.ObservedGranary(full.id(), this.team, this.step, 0);
        assertThat(this.provider.decide(this.context(90, Map.of(), List.of(), List.of(), List.of(full))).candidates()).noneMatch(candidate -> candidate.actionType() == ActionType.RECOLTER);
        assertThat(this.provider.decide(this.context(90, Map.of(), List.of(), List.of(), List.of(available))).candidates()).anyMatch(candidate -> candidate.actionType() == ActionType.RECOLTER);
    }

    @Test
    void shouldProjectShelterOnlyForOwnedOrUnclaimedDestinationHouse() {
        PeonDecisionContext.ObservedHouse owned = new PeonDecisionContext.ObservedHouse(new UUID(0, 3), this.actor, this.team, this.step, false, true);
        PeonDecisionContext.ObservedHouse foreign = new PeonDecisionContext.ObservedHouse(owned.id(), new UUID(0, 4), this.team, this.step, false, false);
        PeonDecisionContext.ObservedHouse unclaimed = new PeonDecisionContext.ObservedHouse(owned.id(), null, null, this.step, false, false);
        assertThat(this.move(this.provider.decide(this.context(9, Map.of(), List.of(), List.of(owned), List.of()))).admissible()).isTrue();
        assertThat(this.move(this.provider.decide(this.context(9, Map.of(), List.of(), List.of(unclaimed), List.of()))).admissible()).isTrue();
        assertThat(this.move(this.provider.decide(this.context(9, Map.of(), List.of(), List.of(foreign), List.of()))).exclusionReason()).isEqualTo("issue prévisible létale");
    }

    private PeonDecision.CandidateScore move(PeonDecision decision) {
        return decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).findFirst().orElseThrow();
    }

    private PeonDecisionContext context(int health, Map<String, RememberedCell> map, List<HexCoordinate> sheep, List<PeonDecisionContext.ObservedHouse> houses, List<PeonDecisionContext.ObservedGranary> granaries) {
        PeonDecisionContext.ObservedField field = new PeonDecisionContext.ObservedField(new UUID(0, 5), this.team, this.origin, 10);
        return new PeonDecisionContext(this.actor, this.team, health, 100, 0, 1, this.origin, false, 10, 42, new PeonPersonality(50, 50, 50, 50), Map.of(), map, List.of(), 0, List.of(this.step), 10, 9, TerrainType.PLAIN, 0, 0, 0, 0, false, null, houses, 0, 0, 0, sheep, field, null, List.of(field), granaries, false, false, 1, null);
    }
}
