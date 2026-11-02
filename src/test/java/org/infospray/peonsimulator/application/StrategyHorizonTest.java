package org.infospray.peonsimulator.application;

import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class StrategyHorizonTest {
    @Test
    void shouldBoundInitialAndRenewedCommitmentsByTheWorldEnd() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Peon peon = new Peon(id, "test", id, new HexCoordinate(0, 0));
        World world = new World();
        world.setCurrentRound(96);
        world.setMaxRounds(100);
        world.setPeons(Map.of(id, peon));
        world.addCell(new Cell(peon.getPosition(), TerrainType.PLAIN, 0));
        PeonStrategyPlanner planner = new PeonStrategyPlanner();
        planner.update(world, peon, new GameDefaultsProperties());
        assertThat(peon.getLongTermStrategy().getCommitmentUntilRound()).isEqualTo(100);
        assertThat(peon.getLongTermStrategy().getTacticalUntilRound()).isLessThanOrEqualTo(100);
        peon.getLongTermStrategy().setCommitmentUntilRound(96);
        peon.getLongTermStrategy().setLastEvaluatedAtRound(90);
        planner.update(world, peon, new GameDefaultsProperties());
        assertThat(peon.getLongTermStrategy().getCommitmentUntilRound()).isEqualTo(100);
    }
}
