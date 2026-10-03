package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.action.PeonAction;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.Cell;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.PeonRelation;
import org.infospray.peonsimulator.domain.model.RememberedPeonObservation;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedPeonDecisionProviderTest {
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties());

    @Test
    void shouldEatToSurviveWhenFoodIsVisible() {
        UUID peonId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext context = new PeonDecisionContext(peonId, teamId, 45, 0, 1, new HexCoordinate(2, 2), false, 1, 42, Map.of(), List.of(), 2, List.of(new HexCoordinate(3, 2)));
        PeonAction action = this.provider.decide(context);
        assertThat(action.type()).isEqualTo(ActionType.MANGER);
    }

    @Test
    void shouldUseOnlyRememberedFoodToChooseItsDirection() {
        UUID peonId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Cell knownFood = new Cell(new HexCoordinate(4, 2), TerrainType.PLAIN, 2);
        RememberedCell memory = new RememberedCell(knownFood, false, 1);
        PeonDecisionContext context = new PeonDecisionContext(peonId, teamId, 60, 0, 1, new HexCoordinate(2, 2), false, 2, 42, Map.of("4:2", memory), List.of(), 0, List.of(new HexCoordinate(3, 2), new HexCoordinate(2, 3)));
        PeonAction action = this.provider.decide(context);
        assertThat(action.type()).isEqualTo(ActionType.SE_DEPLACER);
        assertThat(action.destination()).isEqualTo(new HexCoordinate(3, 2));
    }

    @Test
    void shouldFleeFromRememberedEnemyWhenHealthIsLow() {
        UUID peonId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID enemyId = UUID.randomUUID();
        HexCoordinate enemyPosition = new HexCoordinate(3, 2);
        Cell enemyCell = new Cell(enemyPosition, TerrainType.PLAIN, 0);
        RememberedPeonObservation enemy = new RememberedPeonObservation(enemyId, UUID.randomUUID(), PeonRelation.ENEMY, enemyPosition, 80, 2, 4);
        RememberedCell memory = new RememberedCell(enemyCell, false, 4, List.of(enemy));
        PeonDecisionContext context = new PeonDecisionContext(peonId, teamId, 40, 0, 1, new HexCoordinate(2, 2), false, 5, 42, Map.of("3:2", memory), List.of(), 0, List.of(new HexCoordinate(1, 2), new HexCoordinate(2, 3)));
        PeonAction action = this.provider.decide(context);
        assertThat(action.type()).isEqualTo(ActionType.SE_DEPLACER);
        assertThat(action.destination()).isEqualTo(new HexCoordinate(1, 2));
        assertThat(action.purpose()).isEqualTo(org.infospray.peonsimulator.domain.model.PeonPurpose.SURVIVRE);
    }

    @Test
    void shouldPursueRememberedEnemyWhenRiskIsAcceptable() {
        UUID peonId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID enemyId = UUID.randomUUID();
        HexCoordinate enemyPosition = new HexCoordinate(4, 2);
        Cell enemyCell = new Cell(enemyPosition, TerrainType.PLAIN, 0);
        RememberedPeonObservation enemy = new RememberedPeonObservation(enemyId, UUID.randomUUID(), PeonRelation.ENEMY, enemyPosition, 50, 1, 4);
        RememberedCell memory = new RememberedCell(enemyCell, false, 4, List.of(enemy));
        PeonDecisionContext context = new PeonDecisionContext(peonId, teamId, 90, 0, 2, new HexCoordinate(2, 2), false, 5, 42, Map.of("4:2", memory), List.of(), 0, List.of(new HexCoordinate(3, 2), new HexCoordinate(2, 3)));
        PeonAction action = this.provider.decide(context);
        assertThat(action.type()).isEqualTo(ActionType.SE_DEPLACER);
        assertThat(action.destination()).isEqualTo(new HexCoordinate(3, 2));
        assertThat(action.purpose()).isEqualTo(org.infospray.peonsimulator.domain.model.PeonPurpose.GAGNER_DES_NIVEAUX);
    }

    @Test
    void shouldAttackEnemyOnCurrentCellForExperienceWhenRiskIsAcceptable() {
        UUID peonId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID enemyId = UUID.randomUUID();
        PeonDecisionContext.ObservedPeon enemy = new PeonDecisionContext.ObservedPeon(enemyId, UUID.randomUUID(), 50, 1);
        PeonDecisionContext context = new PeonDecisionContext(peonId, teamId, 90, 0, 2, new HexCoordinate(2, 2), false, 5, 42, Map.of(), List.of(enemy), 0, List.of(new HexCoordinate(3, 2)));
        PeonAction action = this.provider.decide(context);
        assertThat(action.type()).isEqualTo(ActionType.ATTAQUER);
        assertThat(action.targetPeonId()).isEqualTo(enemyId);
    }

    @Test
    void shouldStayConcealedInTreesWhenDangerousEnemyWasRecentlySeen() {
        UUID peonId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        HexCoordinate enemyPosition = new HexCoordinate(3, 2);
        Cell enemyCell = new Cell(enemyPosition, TerrainType.PLAIN, 0);
        RememberedPeonObservation enemy = new RememberedPeonObservation(UUID.randomUUID(), UUID.randomUUID(), PeonRelation.ENEMY, enemyPosition, 90, 3, 9);
        RememberedCell memory = new RememberedCell(enemyCell, false, 9, List.of(enemy));
        PeonDecisionContext context = new PeonDecisionContext(peonId, teamId, 40, 0, 1, new HexCoordinate(2, 2), true, 10, 42, Map.of("3:2", memory), List.of(), 0, List.of(new HexCoordinate(1, 2)));
        PeonAction action = this.provider.decide(context);
        assertThat(action.type()).isEqualTo(ActionType.VOIR);
        assertThat(action.reason()).contains("couvert dans les arbres");
    }
}
