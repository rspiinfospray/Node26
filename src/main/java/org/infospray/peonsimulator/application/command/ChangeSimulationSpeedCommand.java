package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.SimulationSpeedResponse;
import org.infospray.peonsimulator.application.port.secondary.SimulationClock;
import org.springframework.stereotype.Component;

@Component
public class ChangeSimulationSpeedCommand {
    private final SimulationClock simulationClock;

    public ChangeSimulationSpeedCommand(SimulationClock simulationClock) { this.simulationClock = simulationClock; }

    public SimulationSpeedResponse execute(int multiplier) {
        this.simulationClock.setSpeedMultiplier(multiplier);
        return new SimulationSpeedResponse(this.simulationClock.getSpeedMultiplier(), this.simulationClock.getDelayMilliseconds());
    }
}
