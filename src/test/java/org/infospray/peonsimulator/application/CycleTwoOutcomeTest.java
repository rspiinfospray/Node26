package org.infospray.peonsimulator.application;

import org.infospray.peonsimulator.application.dto.ActionRequest;
import org.infospray.peonsimulator.application.port.secondary.*;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.action.PeonAction;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CycleTwoOutcomeTest {
    @Test
    void shouldExcludeBothLevelHealingPhasesFromTheActionReward() {
        Fixture fixture = this.fixture(ActionType.COUPER_DU_BOIS);
        fixture.actor().setExperiencePoints(90);
        fixture.actor().setHealthPoints(20);
        fixture.actor().beginDecision("test", ActionType.COUPER_DU_BOIS);
        fixture.illuvatar().processAction(fixture.request());
        SimulationEvent learned = fixture.events().stream().filter(event -> event.eventType().equals("PEON_LEARNED")).findFirst().orElseThrow();
        assertThat(((Number) learned.payload().get("levelUpHealthGain")).intValue()).isEqualTo(95);
        assertThat(((Number) learned.payload().get("actionHealthDelta")).intValue()).isEqualTo(-5);
    }

    @Test
    void shouldPreserveTheEffectiveWorldHungerReserveDuringATrade() {
        Fixture fixture = this.fixture(ActionType.TROQUER_NOURRITURE_CONTRE_BOIS);
        fixture.world().setHungerHealthLossPerTurn(20);
        fixture.actor().addItem(ItemType.FOOD, 50);
        Peon partner = new Peon(fixture.request().action().targetPeonId(), "partner", fixture.actor().getTeamId(), fixture.actor().getPosition());
        partner.addItem(ItemType.WOOD, 100);
        fixture.world().getPeons().put(partner.getId(), partner);
        fixture.world().cell(partner.getPosition()).addOccupant(partner.getId());
        fixture.illuvatar().processAction(fixture.request());
        assertThat(fixture.actor().itemCount(ItemType.FOOD)).isEqualTo(40);
        assertThat(fixture.actor().itemCount(ItemType.WOOD)).isEqualTo(10);
        assertThat(partner.itemCount(ItemType.FOOD)).isEqualTo(10);
    }

    private Fixture fixture(ActionType type) {
        UUID worldId = new UUID(0, 1);
        UUID actorId = new UUID(0, 2);
        UUID partnerId = new UUID(0, 3);
        UUID actionId = new UUID(0, 4);
        Peon actor = new Peon(actorId, "test", new UUID(0, 5), new HexCoordinate(0, 0));
        World world = new World();
        world.setId(worldId);
        world.setMaxRounds(100);
        world.setHungerHealthLossPerTurn(5);
        world.setSequenceNumber(1);
        world.setPendingActionId(actionId);
        Cell cell = new Cell(actor.getPosition(), TerrainType.TREE, 0);
        cell.setTreeHealthPoints(100);
        cell.addOccupant(actorId);
        world.addCell(cell);
        world.getPeons().put(actorId, actor);
        WorldRepository worlds = mock(WorldRepository.class);
        when(worlds.findById(worldId)).thenReturn(Optional.of(world));
        EventRepository repository = mock(EventRepository.class);
        List<SimulationEvent> events = new ArrayList<>();
        doAnswer(call -> { events.addAll(call.getArgument(0)); return null; }).when(repository).appendAll(anyList());
        Illuvatar illuvatar = new Illuvatar(worlds, repository, mock(ActionRequestPublisher.class), mock(DomainEventPublisher.class), mock(PeonDecisionProvider.class), mock(PeonFirstNameCatalog.class), new GameDefaultsProperties());
        PeonAction action = new PeonAction(type, null, partnerId, PeonPurpose.SURVIVRE, "test");
        return new Fixture(illuvatar, world, actor, new ActionRequest(actionId, worldId, actorId, 1, action), events);
    }

    private record Fixture(Illuvatar illuvatar, World world, Peon actor, ActionRequest request, List<SimulationEvent> events) { }
}
