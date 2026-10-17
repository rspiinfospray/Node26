package org.infospray.peonsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PeonInventoryTest {
    @Test
    void shouldLimitEveryResourceToThreeHundredUnits() {
        Peon peon = new Peon(UUID.randomUUID(), "Faramir", UUID.randomUUID(), new HexCoordinate(0, 0));

        assertThat(peon.addItem(ItemType.WOOD, 350)).isEqualTo(300);
        assertThat(peon.addItem(ItemType.FOOD, 325)).isEqualTo(300);
        assertThat(peon.itemCount(ItemType.WOOD)).isEqualTo(300);
        assertThat(peon.itemCount(ItemType.FOOD)).isEqualTo(300);
    }

    @Test
    void shouldTransferOnlyWhatTheRecipientCanStore() {
        Peon donor = new Peon(UUID.randomUUID(), "Donneur", UUID.randomUUID(), new HexCoordinate(0, 0));
        Peon recipient = new Peon(UUID.randomUUID(), "Receveur", donor.getTeamId(), new HexCoordinate(0, 0));
        donor.addItem(ItemType.FOOD, 100);
        recipient.addItem(ItemType.FOOD, 290);

        assertThat(donor.transferItemTo(recipient, ItemType.FOOD, 40)).isEqualTo(10);
        assertThat(donor.itemCount(ItemType.FOOD)).isEqualTo(90);
        assertThat(recipient.itemCount(ItemType.FOOD)).isEqualTo(300);
    }
}
