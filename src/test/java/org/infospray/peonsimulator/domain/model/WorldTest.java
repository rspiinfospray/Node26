package org.infospray.peonsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorldTest {
    @Test
    void shouldCreateMissingGraveWhenLoadingLegacyDeadPeon() {
        World world = new World();
        Peon deadPeon = new Peon(UUID.randomUUID(), "Boromir", UUID.randomUUID(), new HexCoordinate(2, 3));
        deadPeon.setHealthPoints(0);

        world.setPeons(Map.of(deadPeon.getId(), deadPeon));

        assertThat(world.getGraves()).containsEntry(deadPeon.getId(), new Grave(deadPeon.getId(), deadPeon.getPosition(), 1, 0));
    }
}
