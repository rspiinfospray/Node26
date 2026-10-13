package org.infospray.peonsimulator.application.dto;

import java.time.Instant;
import java.util.UUID;

public record WorldBehaviorReportStatus(UUID worldId, Status status, String fileName, Instant generatedAt, String error) {
    public enum Status { NOT_GENERATED, GENERATING, GENERATED, FAILED }
}
