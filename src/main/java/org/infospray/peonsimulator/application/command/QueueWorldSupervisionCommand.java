package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.WorldBehaviorReportStatus;
import org.infospray.peonsimulator.application.port.secondary.WorldBehaviorReportRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class QueueWorldSupervisionCommand {
    private final WorldBehaviorReportRepository reportRepository;

    public QueueWorldSupervisionCommand(WorldBehaviorReportRepository reportRepository) { this.reportRepository = reportRepository; }

    public WorldBehaviorReportStatus execute(UUID worldId) {
        this.reportRepository.queueForSupervision(worldId);
        return this.reportRepository.status(worldId);
    }
}
