package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.SimulationSpeedResponse;
import org.infospray.peonsimulator.application.port.secondary.SimulationClock;
import org.springframework.stereotype.Component;

@Component
public class GetSimulationSpeedCommand {
    private final SimulationClock simulationClock;

    public GetSimulationSpeedCommand(SimulationClock simulationClock) { this.simulationClock = simulationClock; }

    public SimulationSpeedResponse execute() { return new SimulationSpeedResponse(this.simulationClock.getSpeedMultiplier(), this.simulationClock.getDelayMilliseconds()); }
}
