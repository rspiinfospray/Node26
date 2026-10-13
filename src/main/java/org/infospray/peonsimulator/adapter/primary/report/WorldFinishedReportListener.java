package org.infospray.peonsimulator.adapter.primary.report;

import org.infospray.peonsimulator.application.command.GenerateWorldBehaviorReportCommand;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class WorldFinishedReportListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldFinishedReportListener.class);
    private final GenerateWorldBehaviorReportCommand generateReportCommand;

    public WorldFinishedReportListener(GenerateWorldBehaviorReportCommand generateReportCommand) { this.generateReportCommand = generateReportCommand; }

    @Async
    @EventListener(condition = "#root.event.eventType() == 'WORLD_FINISHED'")
    public void onWorldFinished(SimulationEvent event) {
        try { this.generateReportCommand.execute(event.worldId()); } catch (RuntimeException exception) { LOGGER.error("Échec de génération du rapport comportemental du monde {}: {}", event.worldId(), exception.getMessage()); }
    }
}
