package org.infospray.peonsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorldConstructionInfluenceTest {
    @Test
    void shouldDetectEnemyHouseInfluenceWithinTwoCells() {
        World world = new World();
        UUID alliedTeamId = UUID.randomUUID();
        UUID enemyTeamId = UUID.randomUUID();
        Peon enemyOwner = new Peon(UUID.randomUUID(), "Ennemi", enemyTeamId, new HexCoordinate(2, 0));
        House enemyHouse = new House(UUID.randomUUID(), enemyOwner.getPosition(), enemyOwner.getId());
        world.getPeons().put(enemyOwner.getId(), enemyOwner);
        world.getHouses().put(enemyHouse.getId(), enemyHouse);

        assertThat(world.isInsideEnemyConstructionInfluence(new HexCoordinate(0, 0), alliedTeamId, 2)).isTrue();
        assertThat(world.isInsideEnemyConstructionInfluence(new HexCoordinate(-1, 0), alliedTeamId, 2)).isFalse();
    }

    @Test
    void shouldDetectEnemyFieldAndGranaryInfluenceWithinTwoCells() {
        World world = new World();
        UUID alliedTeamId = UUID.randomUUID();
        UUID enemyTeamId = UUID.randomUUID();
        WheatField enemyField = new WheatField(UUID.randomUUID(), new HexCoordinate(1, 0), enemyTeamId, 10);
        Granary enemyGranary = new Granary(UUID.randomUUID(), new HexCoordinate(0, 2), enemyTeamId);
        world.getWheatFields().put(enemyField.getId(), enemyField);
        world.getGranaries().put(enemyGranary.getId(), enemyGranary);

        assertThat(world.isInsideEnemyConstructionInfluence(new HexCoordinate(0, 0), alliedTeamId, 2)).isTrue();
    }

    @Test
    void shouldAllowAlliedConstructionsWithinInfluenceRadius() {
        World world = new World();
        UUID teamId = UUID.randomUUID();
        Peon owner = new Peon(UUID.randomUUID(), "Allié", teamId, new HexCoordinate(1, 0));
        House alliedHouse = new House(UUID.randomUUID(), owner.getPosition(), owner.getId());
        WheatField alliedField = new WheatField(UUID.randomUUID(), new HexCoordinate(0, 1), teamId, 10);
        Granary alliedGranary = new Granary(UUID.randomUUID(), new HexCoordinate(-1, 1), teamId);
        world.getPeons().put(owner.getId(), owner);
        world.getHouses().put(alliedHouse.getId(), alliedHouse);
        world.getWheatFields().put(alliedField.getId(), alliedField);
        world.getGranaries().put(alliedGranary.getId(), alliedGranary);

        assertThat(world.isInsideEnemyConstructionInfluence(new HexCoordinate(0, 0), teamId, 2)).isFalse();
    }
}
