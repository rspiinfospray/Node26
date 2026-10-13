package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.WorldBehaviorReportStatus;
import org.infospray.peonsimulator.application.dto.WorldBehaviorReportResponse;
import org.infospray.peonsimulator.application.port.secondary.WorldBehaviorReportRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GetWorldBehaviorReportCommand {
    private final WorldBehaviorReportRepository reportRepository;

    public GetWorldBehaviorReportCommand(WorldBehaviorReportRepository reportRepository) { this.reportRepository = reportRepository; }
    public WorldBehaviorReportResponse execute(UUID worldId) {
        WorldBehaviorReportStatus status = this.reportRepository.status(worldId);
        return new WorldBehaviorReportResponse(status, this.reportRepository.read(worldId).orElse(null));
    }
}
