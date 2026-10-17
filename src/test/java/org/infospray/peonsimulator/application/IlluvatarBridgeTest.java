package org.infospray.peonsimulator.application;

import org.infospray.peonsimulator.application.dto.ActionRequest;
import org.infospray.peonsimulator.application.port.secondary.ActionRequestPublisher;
import org.infospray.peonsimulator.application.port.secondary.DomainEventPublisher;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.PeonDecisionProvider;
import org.infospray.peonsimulator.application.port.secondary.PeonFirstNameCatalog;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.action.PeonAction;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.Cell;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.ItemType;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.PeonPurpose;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.infospray.peonsimulator.domain.model.World;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IlluvatarBridgeTest {
    @Test
    void shouldGrantExperienceWhenBridgeIsBuilt() {
        BridgeFixture fixture = this.fixture();

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.world().cell(fixture.destination()).getTerrain()).isEqualTo(TerrainType.BRIDGE);
        assertThat(fixture.peon().getExperiencePoints()).isEqualTo(50);
        verify(fixture.eventRepository()).appendAll(argThat(events -> events.stream().anyMatch(event -> "PEON_EXPERIENCE_GAINED".equals(event.eventType()) && Integer.valueOf(50).equals(event.payload().get("amount")))));
    }

    @Test
    void shouldRejectBridgeWhenOppositeBankIsRock() {
        BridgeFixture fixture = this.fixture();
        HexCoordinate oppositeBank = new HexCoordinate(2, 0);
        fixture.world().addCell(new Cell(oppositeBank, TerrainType.ROCK, 0));

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.world().cell(fixture.destination()).getTerrain()).isEqualTo(TerrainType.RIVER);
        assertThat(fixture.peon().itemCount(ItemType.WOOD)).isEqualTo(50);
        verify(fixture.eventRepository()).appendAll(argThat(events -> events.stream().anyMatch(event -> "PEON_ACTION_REJECTED".equals(event.eventType()) && "BRIDGE_EXIT_BLOCKED_BY_ROCK".equals(event.payload().get("reason")))));
    }

    @Test
    void shouldRejectBridgeWhenAnotherBridgeIsVisible() {
        BridgeFixture fixture = this.fixture();
        fixture.world().addCell(new Cell(new HexCoordinate(0, 1), TerrainType.BRIDGE, 0));

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.world().cell(fixture.destination()).getTerrain()).isEqualTo(TerrainType.RIVER);
        assertThat(fixture.peon().itemCount(ItemType.WOOD)).isEqualTo(50);
        verify(fixture.eventRepository()).appendAll(argThat(events -> events.stream().anyMatch(event -> "PEON_ACTION_REJECTED".equals(event.eventType()) && "BRIDGE_ALREADY_VISIBLE".equals(event.payload().get("reason")))));
    }

    private BridgeFixture fixture() {
        WorldRepository worldRepository = mock(WorldRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        UUID worldId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        HexCoordinate position = new HexCoordinate(0, 0);
        HexCoordinate destination = new HexCoordinate(1, 0);
        Peon peon = new Peon(UUID.randomUUID(), "Celebrimbor", UUID.randomUUID(), position);
        peon.addItem(ItemType.WOOD, 50);
        World world = new World();
        world.setId(worldId);
        world.setMaxRounds(100);
        world.setHungerHealthLossPerTurn(5);
        world.setSequenceNumber(1);
        world.setPendingActionId(actionId);
        Cell actorCell = new Cell(position, TerrainType.PLAIN, 0);
        actorCell.addOccupant(peon.getId());
        world.addCell(actorCell);
        world.addCell(new Cell(destination, TerrainType.RIVER, 0));
        world.addCell(new Cell(new HexCoordinate(2, 0), TerrainType.PLAIN, 0));
        world.getPeons().put(peon.getId(), peon);
        when(worldRepository.findById(worldId)).thenReturn(Optional.of(world));
        when(eventRepository.actionWasProcessed(actionId)).thenReturn(false);
        Illuvatar illuvatar = new Illuvatar(worldRepository, eventRepository, mock(ActionRequestPublisher.class), mock(DomainEventPublisher.class), mock(PeonDecisionProvider.class), mock(PeonFirstNameCatalog.class), new GameDefaultsProperties());
        PeonAction action = new PeonAction(ActionType.CONSTRUIRE_PONT, destination, null, PeonPurpose.AIDER_LES_ALLIES, "test");
        return new BridgeFixture(illuvatar, eventRepository, world, peon, destination, new ActionRequest(actionId, worldId, peon.getId(), 1, action));
    }

    private record BridgeFixture(Illuvatar illuvatar, EventRepository eventRepository, World world, Peon peon, HexCoordinate destination, ActionRequest request) {
    }
}
