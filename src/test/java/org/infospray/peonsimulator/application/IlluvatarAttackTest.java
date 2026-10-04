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

class IlluvatarAttackTest {
    @Test
    void shouldGiveSixtyExperienceForLethalAttack() {
        AttackFixture fixture = this.fixture(20);

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.actor().getExperiencePoints()).isEqualTo(60);
        assertThat(fixture.target().getExperiencePoints()).isZero();
        assertThat(fixture.target().isAlive()).isFalse();
    }

    @Test
    void shouldKeepFortyExperienceForNonLethalAttack() {
        AttackFixture fixture = this.fixture(21);

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.actor().getExperiencePoints()).isEqualTo(40);
        assertThat(fixture.target().getExperiencePoints()).isEqualTo(40);
        assertThat(fixture.target().isAlive()).isTrue();
    }

    @Test
    void shouldNotResurrectTargetWhenAttackWouldHaveGrantedALevel() {
        AttackFixture fixture = this.fixture(20);
        fixture.target().setExperiencePoints(90);

        fixture.illuvatar().processAction(fixture.request());

        assertThat(fixture.target().isAlive()).isFalse();
        assertThat(fixture.target().getHealthPoints()).isZero();
        assertThat(fixture.target().getExperiencePoints()).isEqualTo(90);
        assertThat(fixture.target().getLevel()).isEqualTo(1);
    }

    private AttackFixture fixture(int targetHealth) {
        WorldRepository worldRepository = mock(WorldRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        GameDefaultsProperties defaults = new GameDefaultsProperties();
        UUID worldId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        UUID actorTeamId = UUID.randomUUID();
        UUID targetTeamId = UUID.randomUUID();
        HexCoordinate position = new HexCoordinate(0, 0);
        Peon actor = new Peon(UUID.randomUUID(), "Aragorn", actorTeamId, position);
        Peon target = new Peon(UUID.randomUUID(), "Azog", targetTeamId, position);
        target.setHealthPoints(targetHealth);
        World world = new World();
        world.setId(worldId);
        world.setMaxRounds(100);
        world.setSequenceNumber(1);
        world.setPendingActionId(actionId);
        Cell cell = new Cell(position, TerrainType.PLAIN, 0);
        cell.addOccupant(actor.getId());
        cell.addOccupant(target.getId());
        world.addCell(cell);
        world.getPeons().put(actor.getId(), actor);
        world.getPeons().put(target.getId(), target);
        when(worldRepository.findById(worldId)).thenReturn(Optional.of(world));
        when(eventRepository.actionWasProcessed(actionId)).thenReturn(false);
        Illuvatar illuvatar = new Illuvatar(worldRepository, eventRepository, mock(ActionRequestPublisher.class), mock(DomainEventPublisher.class), mock(PeonDecisionProvider.class), mock(PeonFirstNameCatalog.class), defaults);
        PeonAction action = new PeonAction(ActionType.ATTAQUER, null, target.getId(), PeonPurpose.GAGNER_DES_NIVEAUX, "test");
        return new AttackFixture(illuvatar, new ActionRequest(actionId, worldId, actor.getId(), 1, action), actor, target);
    }

    private record AttackFixture(Illuvatar illuvatar, ActionRequest request, Peon actor, Peon target) {
    }
}
