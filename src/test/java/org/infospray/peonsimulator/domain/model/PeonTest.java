package org.infospray.peonsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PeonTest {
    @Test
    void shouldIncreaseCombatStatisticsForEveryLevelGained() {
        Peon peon = new Peon(UUID.randomUUID(), "Aragorn", UUID.randomUUID(), new HexCoordinate(1, 1));
        int previousLevel = peon.gainExperience(200, 10, 10);
        assertThat(previousLevel).isEqualTo(1);
        assertThat(peon.getLevel()).isEqualTo(3);
        assertThat(peon.getMaxHealthPoints()).isEqualTo(120);
        assertThat(peon.getHealthPoints()).isEqualTo(100);
        assertThat(peon.getAttackDamage()).isEqualTo(40);
    }
}
