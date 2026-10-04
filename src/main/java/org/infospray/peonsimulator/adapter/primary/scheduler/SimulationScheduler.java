package org.infospray.peonsimulator.adapter.primary.scheduler;

import org.infospray.peonsimulator.application.command.AdvanceWorldTurnCommand;
import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.application.port.secondary.SimulationClock;
import org.infospray.peonsimulator.domain.model.WorldStatus;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SimulationScheduler implements SchedulingConfigurer {
    private final Illuvatar illuvatar;
    private final AdvanceWorldTurnCommand advanceWorldTurnCommand;
    private final SimulationClock simulationClock;

    public SimulationScheduler(Illuvatar illuvatar, AdvanceWorldTurnCommand advanceWorldTurnCommand, SimulationClock simulationClock) {
        this.illuvatar = illuvatar;
        this.advanceWorldTurnCommand = advanceWorldTurnCommand;
        this.simulationClock = simulationClock;
    }

    public void advanceRunningWorlds() {
        this.illuvatar.getWorlds().stream().filter(world -> world.getStatus() == WorldStatus.RUNNING).forEach(world -> this.advanceWorldTurnCommand.execute(world.getId(), false));
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(this::advanceRunningWorlds, triggerContext -> {
            Instant lastCompletion = triggerContext.lastCompletion();
            return (lastCompletion == null ? Instant.now() : lastCompletion).plusMillis(this.simulationClock.getDelayMilliseconds());
        });
    }
}
