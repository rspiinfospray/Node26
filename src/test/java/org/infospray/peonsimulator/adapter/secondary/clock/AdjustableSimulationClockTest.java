package org.infospray.peonsimulator.adapter.secondary.clock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdjustableSimulationClockTest {
    @Test
    void shouldSwitchBetweenNormalAndDoubleSpeed() {
        AdjustableSimulationClock clock = new AdjustableSimulationClock(600);

        assertThat(clock.getSpeedMultiplier()).isEqualTo(1);
        assertThat(clock.getDelayMilliseconds()).isEqualTo(600);

        clock.setSpeedMultiplier(2);

        assertThat(clock.getSpeedMultiplier()).isEqualTo(2);
        assertThat(clock.getDelayMilliseconds()).isEqualTo(300);

        clock.setSpeedMultiplier(1);

        assertThat(clock.getDelayMilliseconds()).isEqualTo(600);
    }

    @Test
    void shouldRejectUnsupportedSpeed() {
        AdjustableSimulationClock clock = new AdjustableSimulationClock(600);

        assertThatThrownBy(() -> clock.setSpeedMultiplier(3)).isInstanceOf(IllegalArgumentException.class);
    }
}
