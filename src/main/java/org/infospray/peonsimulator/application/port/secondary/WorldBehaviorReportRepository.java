package org.infospray.peonsimulator.application.port.secondary;

import org.infospray.peonsimulator.application.dto.WorldBehaviorReportStatus;

import java.util.Optional;
import java.util.UUID;

public interface WorldBehaviorReportRepository {
    WorldBehaviorReportStatus status(UUID worldId);
    void markGenerating(UUID worldId);
    void save(UUID worldId, String worldName, String markdown);
    void markFailed(UUID worldId, String error);
    void queueForSupervision(UUID worldId);
    Optional<String> read(UUID worldId);
}
