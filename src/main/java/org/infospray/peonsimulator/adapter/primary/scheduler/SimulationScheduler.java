package org.infospray.peonsimulator.adapter.primary.scheduler;

import org.infospray.peonsimulator.application.command.AdvanceWorldTurnCommand;
import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.domain.model.WorldStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SimulationScheduler {
    private final Illuvatar illuvatar;
    private final AdvanceWorldTurnCommand advanceWorldTurnCommand;

    public SimulationScheduler(Illuvatar illuvatar, AdvanceWorldTurnCommand advanceWorldTurnCommand) {
        this.illuvatar = illuvatar;
        this.advanceWorldTurnCommand = advanceWorldTurnCommand;
    }

    @Scheduled(fixedDelayString = "${simulator.clock.delay-ms:600}")
    public void advanceRunningWorlds() {
        this.illuvatar.getWorlds().stream().filter(world -> world.getStatus() == WorldStatus.RUNNING).forEach(world -> this.advanceWorldTurnCommand.execute(world.getId(), false));
    }
}
