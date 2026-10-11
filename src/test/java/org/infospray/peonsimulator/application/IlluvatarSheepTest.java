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
import org.infospray.peonsimulator.domain.model.House;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.PeonPurpose;
import org.infospray.peonsimulator.domain.model.Sheep;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.infospray.peonsimulator.domain.model.World;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IlluvatarSheepTest {
    @Test
    void shouldHealAndRewardPeonWhenEatingSheep() {
        SheepFixture fixture = this.fixture(ActionType.MANGER_MOUTON, 0);
        fixture.peon().setHealthPoints(20);

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.world().getSheep()).isEmpty();
        assertThat(fixture.peon().getHealthPoints()).isEqualTo(75);
        assertThat(fixture.peon().getExperiencePoints()).isEqualTo(30);
    }

    @Test
    void shouldMoveSheepOnlyToCellWithoutRockOrHouse() {
        SheepFixture fixture = this.fixture(ActionType.NE_RIEN_FAIRE, 100);
        HexCoordinate validDestination = new HexCoordinate(-1, 0);
        HexCoordinate rockDestination = new HexCoordinate(1, 0);
        HexCoordinate houseDestination = new HexCoordinate(0, 1);
        fixture.world().addCell(new Cell(validDestination, TerrainType.PLAIN, 0));
        fixture.world().addCell(new Cell(rockDestination, TerrainType.ROCK, 0));
        fixture.world().addCell(new Cell(houseDestination, TerrainType.PLAIN, 0));
        House house = new House(UUID.randomUUID(), houseDestination, fixture.peon().getId());
        fixture.world().getHouses().put(house.getId(), house);

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.sheep().getPosition()).isEqualTo(validDestination);
    }

    private SheepFixture fixture(ActionType actionType, int sheepMovementPercentage) {
        WorldRepository worldRepository = mock(WorldRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        GameDefaultsProperties defaults = new GameDefaultsProperties();
        defaults.setSheepMovementPercentage(sheepMovementPercentage);
        UUID worldId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        HexCoordinate position = new HexCoordinate(0, 0);
        Peon peon = new Peon(UUID.randomUUID(), "Borin", UUID.randomUUID(), position);
        Sheep sheep = new Sheep(UUID.randomUUID(), position);
        World world = new World();
        world.setId(worldId);
        world.setMaxRounds(10);
        world.setHungerHealthLossPerTurn(5);
        world.setSeed(42);
        world.setSequenceNumber(1);
        world.setPendingActionId(actionId);
        world.getPeons().put(peon.getId(), peon);
        world.getSheep().put(sheep.getId(), sheep);
        Cell cell = new Cell(position, TerrainType.PLAIN, 0);
        cell.addOccupant(peon.getId());
        world.addCell(cell);
        when(worldRepository.findById(worldId)).thenReturn(Optional.of(world));
        when(eventRepository.actionWasProcessed(actionId)).thenReturn(false);
        Illuvatar illuvatar = new Illuvatar(worldRepository, eventRepository, mock(ActionRequestPublisher.class), mock(DomainEventPublisher.class), mock(PeonDecisionProvider.class), mock(PeonFirstNameCatalog.class), defaults);
        PeonAction action = new PeonAction(actionType, null, null, PeonPurpose.SURVIVRE, "test");
        return new SheepFixture(illuvatar, new ActionRequest(actionId, worldId, peon.getId(), 1, action), world, peon, sheep);
    }

    private record SheepFixture(Illuvatar illuvatar, ActionRequest request, World world, Peon peon, Sheep sheep) {
    }
}
