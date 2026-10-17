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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IlluvatarFoodExchangeTest {
    @Test
    void shouldStoreHealingSurplusAsFood() {
        ExchangeFixture fixture = this.fixture(ActionType.MANGER, null);
        fixture.actor().setHealthPoints(90);
        fixture.world().cell(fixture.actor().getPosition()).setFoodQuantity(1);

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.actor().itemCount(ItemType.FOOD)).isEqualTo(10);
    }

    @Test
    void shouldTradeFoodForWoodAtOneToOneRate() {
        ExchangeFixture fixture = this.fixture(ActionType.TROQUER_NOURRITURE_CONTRE_BOIS, UUID.randomUUID());
        Peon partner = new Peon(fixture.request().action().targetPeonId(), "Partenaire", fixture.actor().getTeamId(), fixture.actor().getPosition());
        fixture.actor().addItem(ItemType.FOOD, 50);
        partner.addItem(ItemType.WOOD, 40);
        fixture.world().getPeons().put(partner.getId(), partner);
        fixture.world().cell(partner.getPosition()).addOccupant(partner.getId());

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.actor().itemCount(ItemType.WOOD)).isEqualTo(35);
        assertThat(fixture.actor().itemCount(ItemType.FOOD)).isEqualTo(15);
        assertThat(partner.itemCount(ItemType.WOOD)).isEqualTo(5);
        assertThat(partner.itemCount(ItemType.FOOD)).isEqualTo(35);
    }

    private ExchangeFixture fixture(ActionType actionType, UUID targetPeonId) {
        WorldRepository worldRepository = mock(WorldRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        UUID worldId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        HexCoordinate position = new HexCoordinate(0, 0);
        Peon actor = new Peon(UUID.randomUUID(), "Eowyn", UUID.randomUUID(), position);
        World world = new World();
        world.setId(worldId);
        world.setMaxRounds(100);
        world.setHungerHealthLossPerTurn(5);
        world.setSequenceNumber(1);
        world.setPendingActionId(actionId);
        Cell cell = new Cell(position, TerrainType.PLAIN, 0);
        cell.addOccupant(actor.getId());
        world.addCell(cell);
        world.getPeons().put(actor.getId(), actor);
        when(worldRepository.findById(worldId)).thenReturn(Optional.of(world));
        when(eventRepository.actionWasProcessed(actionId)).thenReturn(false);
        Illuvatar illuvatar = new Illuvatar(worldRepository, eventRepository, mock(ActionRequestPublisher.class), mock(DomainEventPublisher.class), mock(PeonDecisionProvider.class), mock(PeonFirstNameCatalog.class), new GameDefaultsProperties());
        PeonAction action = new PeonAction(actionType, null, targetPeonId, PeonPurpose.SURVIVRE, "test");
        return new ExchangeFixture(illuvatar, new ActionRequest(actionId, worldId, actor.getId(), 1, action), actor, world);
    }

    private record ExchangeFixture(Illuvatar illuvatar, ActionRequest request, Peon actor, World world) {
    }
}
