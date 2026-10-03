package org.infospray.peonsimulator.adapter.primary.rest;

import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class GameConfigurationController {
    private final GameDefaultsProperties defaults;

    public GameConfigurationController(GameDefaultsProperties defaults) { this.defaults = defaults; }

    @GetMapping
    public GameDefaultsProperties get() { return this.defaults; }
}
