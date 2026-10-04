package org.infospray.peonsimulator.adapter.primary.rest;

import org.infospray.peonsimulator.application.command.ChangeSimulationSpeedCommand;
import org.infospray.peonsimulator.application.command.GetSimulationSpeedCommand;
import org.infospray.peonsimulator.application.dto.SimulationSpeedResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {
    private final GetSimulationSpeedCommand getSimulationSpeedCommand;
    private final ChangeSimulationSpeedCommand changeSimulationSpeedCommand;

    public SimulationController(GetSimulationSpeedCommand getSimulationSpeedCommand, ChangeSimulationSpeedCommand changeSimulationSpeedCommand) {
        this.getSimulationSpeedCommand = getSimulationSpeedCommand;
        this.changeSimulationSpeedCommand = changeSimulationSpeedCommand;
    }

    @GetMapping("/speed")
    public SimulationSpeedResponse getSpeed() { return this.getSimulationSpeedCommand.execute(); }

    @PostMapping("/speed")
    public SimulationSpeedResponse changeSpeed(@RequestParam int multiplier) { return this.changeSimulationSpeedCommand.execute(multiplier); }
}
