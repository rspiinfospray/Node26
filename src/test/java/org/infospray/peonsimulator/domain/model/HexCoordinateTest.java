package org.infospray.peonsimulator.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HexCoordinateTest {
    @Test
    void shouldExposeSixNeighborsAndCorrectDistance() {
        HexCoordinate origin = new HexCoordinate(0, 0);
        assertThat(origin.neighbors()).hasSize(6).contains(new HexCoordinate(1, 0), new HexCoordinate(-1, 1));
        assertThat(origin.distanceTo(new HexCoordinate(2, -1))).isEqualTo(2);
    }
}
