package org.infospray.peonsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorldTest {
    @Test
    void shouldFinishAtConfiguredMaximumRound() {
        World world = new World();
        Peon peon = new Peon(UUID.randomUUID(), "Aragorn", UUID.randomUUID(), new HexCoordinate(0, 0));
        world.setPeons(Map.of(peon.getId(), peon));
        world.setCurrentRound(100);
        world.setMaxRounds(100);

        world.completeTurn();

        assertThat(world.getCurrentRound()).isEqualTo(100);
        assertThat(world.getStatus()).isEqualTo(WorldStatus.FINISHED);
    }

    @Test
    void shouldCreateMissingGraveWhenLoadingLegacyDeadPeon() {
        World world = new World();
        Peon deadPeon = new Peon(UUID.randomUUID(), "Boromir", UUID.randomUUID(), new HexCoordinate(2, 3));
        deadPeon.setHealthPoints(0);

        world.setPeons(Map.of(deadPeon.getId(), deadPeon));

        assertThat(world.getGraves()).containsEntry(deadPeon.getId(), new Grave(deadPeon.getId(), deadPeon.getPosition(), 1, 0));
    }
}
