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

class IlluvatarRejectedActionTest {
    @Test
    void shouldNotApplyHungerWhenActionIsRejected() {
        WorldRepository worldRepository = mock(WorldRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        UUID worldId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        HexCoordinate position = new HexCoordinate(0, 0);
        Peon peon = new Peon(UUID.randomUUID(), "Eldamir", UUID.randomUUID(), position);
        World world = new World();
        world.setId(worldId);
        world.setMaxRounds(100);
        world.setHungerHealthLossPerTurn(5);
        world.setSequenceNumber(1);
        world.setPendingActionId(actionId);
        Cell cell = new Cell(position, TerrainType.PLAIN, 0);
        cell.addOccupant(peon.getId());
        world.addCell(cell);
        world.getPeons().put(peon.getId(), peon);
        when(worldRepository.findById(worldId)).thenReturn(Optional.of(world));
        when(eventRepository.actionWasProcessed(actionId)).thenReturn(false);
        Illuvatar illuvatar = new Illuvatar(worldRepository, eventRepository, mock(ActionRequestPublisher.class), mock(DomainEventPublisher.class), mock(PeonDecisionProvider.class), mock(PeonFirstNameCatalog.class), new GameDefaultsProperties());
        PeonAction impossibleMeal = new PeonAction(ActionType.MANGER, null, null, PeonPurpose.SURVIVRE, "test");

        illuvatar.processAction(new ActionRequest(actionId, worldId, peon.getId(), 1, impossibleMeal));

        assertThat(peon.getHealthPoints()).isEqualTo(100);
    }
}
