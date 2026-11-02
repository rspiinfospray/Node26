package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.WorldAnalysisContextResponse;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.model.Granary;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.House;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.Sheep;
import org.infospray.peonsimulator.domain.model.WheatField;
import org.infospray.peonsimulator.domain.model.World;
import org.infospray.peonsimulator.domain.model.WorldStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetWorldAnalysisContextCommandTest {
    @Test
    void shouldReturnCompactWorldContext() {
        WorldRepository worldRepository = mock(WorldRepository.class);
        UUID worldId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Peon livingPeon = new Peon(UUID.randomUUID(), "Aragorn", teamId, new HexCoordinate(0, 0));
        Peon deadPeon = new Peon(UUID.randomUUID(), "Boromir", teamId, new HexCoordinate(1, 0));
        deadPeon.setHealthPoints(0);
        World world = new World();
        world.setId(worldId);
        world.setName("Gondor");
        world.setStatus(WorldStatus.FINISHED);
        world.setWidth(50);
        world.setHeight(40);
        world.setSeed(42);
        world.setRockPercentage(20);
        world.setTreePercentage(15);
        world.setFoodCellPercentage(10);
        world.setSheepCount(10);
        world.setRiverGenerationEnabled(true);
        world.setCurrentRound(100);
        world.setMaxRounds(100);
        world.setSequenceNumber(500);
        world.setPeons(Map.of(livingPeon.getId(), livingPeon, deadPeon.getId(), deadPeon));
        world.setHouses(Map.of(UUID.randomUUID(), mock(House.class)));
        world.setGranaries(Map.of(UUID.randomUUID(), mock(Granary.class)));
        world.setWheatFields(Map.of(UUID.randomUUID(), mock(WheatField.class)));
        world.setSheep(Map.of(UUID.randomUUID(), mock(Sheep.class)));
        when(worldRepository.findById(worldId)).thenReturn(Optional.of(world));

        WorldAnalysisContextResponse response = new GetWorldAnalysisContextCommand(worldRepository).execute(worldId);

        assertThat(response.id()).isEqualTo(worldId);
        assertThat(response.population()).isEqualTo(new WorldAnalysisContextResponse.Population(2, 1, 1));
        assertThat(response.constructions()).isEqualTo(new WorldAnalysisContextResponse.Constructions(1, 1, 1));
        assertThat(response.remainingSheep()).isEqualTo(1);
        assertThat(response.generation().initialSheep()).isEqualTo(10);
    }
}
