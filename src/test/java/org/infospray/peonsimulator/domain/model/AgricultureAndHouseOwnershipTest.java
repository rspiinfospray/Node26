package org.infospray.peonsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgricultureAndHouseOwnershipTest {
    @Test
    void fieldRequiresAnAlliedGranaryWithinInfluenceRadius() {
        UUID teamId = UUID.randomUUID();
        World world = new World();
        Granary alliedAtLimit = new Granary(UUID.randomUUID(), new HexCoordinate(2, 0), teamId);
        Granary enemyNearby = new Granary(UUID.randomUUID(), new HexCoordinate(1, 0), UUID.randomUUID());
        world.getGranaries().put(alliedAtLimit.getId(), alliedAtLimit);
        world.getGranaries().put(enemyNearby.getId(), enemyNearby);
        assertThat(world.hasAlliedGranaryWithin(new HexCoordinate(0, 0), teamId, 2)).isTrue();
        assertThat(world.closestAlliedGranaryWithin(new HexCoordinate(0, 0), teamId, 2)).isEqualTo(alliedAtLimit);
        alliedAtLimit.setPosition(new HexCoordinate(3, 0));
        assertThat(world.hasAlliedGranaryWithin(new HexCoordinate(0, 0), teamId, 2)).isFalse();
        assertThat(world.closestAlliedGranaryWithin(new HexCoordinate(0, 0), teamId, 2)).isNull();
    }

    @Test
    void houseRemainsOwnedWhenEveryOccupantLeaves() {
        UUID ownerId = UUID.randomUUID();
        UUID coOwnerId = UUID.randomUUID();
        House house = new House(UUID.randomUUID(), new HexCoordinate(2, 3), ownerId);
        house.inviteOwner(coOwnerId);
        house.leave(ownerId);
        house.leave(coOwnerId);
        assertThat(house.isEmpty()).isTrue();
        assertThat(house.isOwned()).isTrue();
        assertThat(house.getOwnerPeonIds()).containsExactlyInAnyOrder(ownerId, coOwnerId);
    }

    @Test
    void houseBecomesUnownedOnlyAfterLastOwnerDies() {
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        House house = new House(UUID.randomUUID(), new HexCoordinate(0, 0), firstOwner);
        house.inviteOwner(secondOwner);
        house.removeOwner(firstOwner);
        assertThat(house.isOwned()).isTrue();
        assertThat(house.getHostPeonId()).isEqualTo(secondOwner);
        house.removeOwner(secondOwner);
        assertThat(house.isOwned()).isFalse();
    }

    @Test
    void harvestFillsGranaryAndSchedulesNextGrowth() {
        WheatField field = new WheatField(UUID.randomUUID(), new HexCoordinate(1, 1), UUID.randomUUID(), 5);
        Granary granary = new Granary(UUID.randomUUID(), new HexCoordinate(1, 2), field.getTeamId());
        field.ripen(5, 5);
        int harvested = field.harvest(50, 5, 8);
        int stored = granary.store(harvested, 50);
        assertThat(stored).isEqualTo(5);
        assertThat(granary.getWheatQuantity()).isEqualTo(5);
        assertThat(field.getReadyAtRound()).isEqualTo(13);
        assertThat(field.isReady()).isFalse();
    }
}
