package org.infospray.peonsimulator.application.dto;

import java.util.UUID;

public record PeonSummaryResponse(UUID id, String name, Status status, Long deathSequence) {
    public enum Status {
        VIVANT,
        MORT
    }
}
