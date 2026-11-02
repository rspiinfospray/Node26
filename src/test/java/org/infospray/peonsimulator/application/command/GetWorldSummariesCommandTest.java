package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.WorldSummaryResponse;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.World;
import org.infospray.peonsimulator.domain.model.WorldStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetWorldSummariesCommandTest {
    @Test
    void shouldReturnOnlyWorldSummaryData() {
        WorldRepository worldRepository = mock(WorldRepository.class);
        UUID worldId = UUID.randomUUID();
        World world = new World();
        world.setId(worldId);
        world.setName("Gondor");
        world.setStatus(WorldStatus.RUNNING);
        world.setCurrentRound(12);
        world.setMaxRounds(100);
        world.setSequenceNumber(42);
        Peon alivePeon = new Peon(UUID.randomUUID(), "Aragorn", UUID.randomUUID(), new HexCoordinate(0, 0));
        Peon deadPeon = new Peon(UUID.randomUUID(), "Boromir", UUID.randomUUID(), new HexCoordinate(1, 0));
        deadPeon.setHealthPoints(0);
        world.setPeons(Map.of(alivePeon.getId(), alivePeon, deadPeon.getId(), deadPeon));
        when(worldRepository.findAll()).thenReturn(List.of(world));

        List<WorldSummaryResponse> summaries = new GetWorldSummariesCommand(worldRepository).execute();

        assertThat(summaries).containsExactly(new WorldSummaryResponse(worldId, "Gondor", WorldStatus.RUNNING, 12, 100, 42, 2, 1));
    }
}
