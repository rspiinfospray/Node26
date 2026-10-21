package org.infospray.peonsimulator.adapter.primary.scheduler;

import org.infospray.peonsimulator.application.command.AdvanceWorldTurnCommand;
import org.infospray.peonsimulator.application.port.secondary.SimulationClock;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.model.World;
import org.infospray.peonsimulator.domain.model.WorldStatus;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class SimulationScheduler implements SchedulingConfigurer {
    private final WorldRepository worldRepository;
    private final AdvanceWorldTurnCommand advanceWorldTurnCommand;
    private final SimulationClock simulationClock;
    private final Map<UUID, Instant> nextActionAtByWorld = new HashMap<>();

    public SimulationScheduler(WorldRepository worldRepository, AdvanceWorldTurnCommand advanceWorldTurnCommand, SimulationClock simulationClock) {
        this.worldRepository = worldRepository;
        this.advanceWorldTurnCommand = advanceWorldTurnCommand;
        this.simulationClock = simulationClock;
    }

    public void advanceRunningWorlds() {
        Instant now = Instant.now();
        Set<UUID> runningWorldIds = Set.copyOf(this.worldRepository.findIdsByStatus(WorldStatus.RUNNING));
        this.nextActionAtByWorld.keySet().retainAll(runningWorldIds);
        for (UUID worldId : runningWorldIds) {
            World world = this.worldRepository.findById(worldId).orElse(null);
            if (world == null || world.getPendingActionId() != null || now.isBefore(this.nextActionAtByWorld.getOrDefault(worldId, now))) { continue; }
            int livingPeonCount = Math.max(1, world.alivePeons().size());
            this.advanceWorldTurnCommand.execute(worldId, false);
            this.nextActionAtByWorld.put(worldId, now.plusMillis(Math.max(1, this.simulationClock.getDelayMilliseconds() / livingPeonCount)));
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(this::advanceRunningWorlds, triggerContext -> {
            Instant lastCompletion = triggerContext.lastCompletion();
            return (lastCompletion == null ? Instant.now() : lastCompletion).plusMillis(10);
        });
    }
}
