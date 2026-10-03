package org.infospray.peonsimulator.domain.model;

import java.util.List;

public record HexCoordinate(int q, int r) {
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, -1}, {-1, 1}};

    public List<HexCoordinate> neighbors() {
        return java.util.Arrays.stream(DIRECTIONS).map(direction -> new HexCoordinate(this.q + direction[0], this.r + direction[1])).toList();
    }

    public int distanceTo(HexCoordinate other) {
        int dq = this.q - other.q;
        int dr = this.r - other.r;
        return (Math.abs(dq) + Math.abs(dq + dr) + Math.abs(dr)) / 2;
    }
}
