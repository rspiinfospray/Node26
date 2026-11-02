package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.PeonSummaryResponse;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.model.Grave;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.World;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetWorldPeonsCommandTest {
    @Test
    void shouldListLivingAndDeadPeonsWithDeathSequence() {
        WorldRepository worldRepository = mock(WorldRepository.class);
        UUID worldId = UUID.randomUUID();
        Peon livingPeon = new Peon(UUID.randomUUID(), "Aragorn", UUID.randomUUID(), new HexCoordinate(0, 0));
        Peon deadPeon = new Peon(UUID.randomUUID(), "Boromir", UUID.randomUUID(), new HexCoordinate(1, 0));
        deadPeon.setHealthPoints(0);
        World world = new World();
        world.setId(worldId);
        world.setPeons(new LinkedHashMap<>(Map.of(livingPeon.getId(), livingPeon, deadPeon.getId(), deadPeon)));
        world.setGraves(Map.of(deadPeon.getId(), new Grave(deadPeon.getId(), deadPeon.getPosition(), 8, 137)));
        when(worldRepository.findById(worldId)).thenReturn(Optional.of(world));

        List<PeonSummaryResponse> peons = new GetWorldPeonsCommand(worldRepository).execute(worldId);

        assertThat(peons).containsExactlyInAnyOrder(
                new PeonSummaryResponse(livingPeon.getId(), "Aragorn", PeonSummaryResponse.Status.VIVANT, null),
                new PeonSummaryResponse(deadPeon.getId(), "Boromir", PeonSummaryResponse.Status.MORT, 137L));
    }
}
