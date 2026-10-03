package org.infospray.peonsimulator.adapter.primary.rest;

import jakarta.validation.Valid;
import org.infospray.peonsimulator.adapter.primary.sse.SimulationEventBroadcaster;
import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.application.command.AdvanceWorldTurnCommand;
import org.infospray.peonsimulator.application.command.AnalyzeWorldCommand;
import org.infospray.peonsimulator.application.command.ChangeWorldStatusCommand;
import org.infospray.peonsimulator.application.command.CreateWorldCommand;
import org.infospray.peonsimulator.application.command.DeleteAllWorldsCommand;
import org.infospray.peonsimulator.application.command.GetWorldStateCommand;
import org.infospray.peonsimulator.application.command.GetWorldTimelineCommand;
import org.infospray.peonsimulator.application.dto.CreateWorldRequest;
import org.infospray.peonsimulator.application.dto.WorldAnalysisResponse;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.World;
import org.infospray.peonsimulator.domain.model.WorldStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/worlds")
public class WorldController {
    private final Illuvatar illuvatar;
    private final CreateWorldCommand createWorldCommand;
    private final AdvanceWorldTurnCommand advanceWorldTurnCommand;
    private final ChangeWorldStatusCommand changeWorldStatusCommand;
    private final GetWorldStateCommand getWorldStateCommand;
    private final GetWorldTimelineCommand getWorldTimelineCommand;
    private final AnalyzeWorldCommand analyzeWorldCommand;
    private final DeleteAllWorldsCommand deleteAllWorldsCommand;
    private final SimulationEventBroadcaster broadcaster;

    public WorldController(Illuvatar illuvatar, CreateWorldCommand createWorldCommand, AdvanceWorldTurnCommand advanceWorldTurnCommand, ChangeWorldStatusCommand changeWorldStatusCommand, GetWorldStateCommand getWorldStateCommand, GetWorldTimelineCommand getWorldTimelineCommand, AnalyzeWorldCommand analyzeWorldCommand, DeleteAllWorldsCommand deleteAllWorldsCommand, SimulationEventBroadcaster broadcaster) {
        this.illuvatar = illuvatar;
        this.createWorldCommand = createWorldCommand;
        this.advanceWorldTurnCommand = advanceWorldTurnCommand;
        this.changeWorldStatusCommand = changeWorldStatusCommand;
        this.getWorldStateCommand = getWorldStateCommand;
        this.getWorldTimelineCommand = getWorldTimelineCommand;
        this.analyzeWorldCommand = analyzeWorldCommand;
        this.deleteAllWorldsCommand = deleteAllWorldsCommand;
        this.broadcaster = broadcaster;
    }

    @PostMapping
    public World create(@Valid @RequestBody CreateWorldRequest request) { return this.createWorldCommand.execute(request); }

    @GetMapping
    public List<World> list() { return this.illuvatar.getWorlds(); }

    @DeleteMapping
    public void deleteAll() { this.deleteAllWorldsCommand.execute(); }

    @GetMapping("/{worldId}")
    public World get(@PathVariable UUID worldId, @RequestParam(required = false) Long sequence) { return this.getWorldStateCommand.execute(worldId, sequence); }

    @PostMapping("/{worldId}/start")
    public World start(@PathVariable UUID worldId) { return this.changeWorldStatusCommand.execute(worldId, WorldStatus.RUNNING); }

    @PostMapping("/{worldId}/pause")
    public World pause(@PathVariable UUID worldId) { return this.changeWorldStatusCommand.execute(worldId, WorldStatus.PAUSED); }

    @PostMapping("/{worldId}/step")
    public World step(@PathVariable UUID worldId) { return this.advanceWorldTurnCommand.execute(worldId, true); }

    @GetMapping("/{worldId}/events")
    public List<SimulationEvent> events(@PathVariable UUID worldId, @RequestParam(defaultValue = "0") long after, @RequestParam(defaultValue = "500") int limit) { return this.getWorldTimelineCommand.execute(worldId, after, limit); }

    @GetMapping("/{worldId}/analysis")
    public WorldAnalysisResponse analysis(@PathVariable UUID worldId) { return this.analyzeWorldCommand.execute(worldId); }

    @GetMapping("/{worldId}/peons/{peonId}/mental-map")
    public Map<String, RememberedCell> mentalMap(@PathVariable UUID worldId, @PathVariable UUID peonId, @RequestParam(required = false) Long sequence) {
        World world = this.getWorldStateCommand.execute(worldId, sequence);
        if (!world.getPeons().containsKey(peonId)) { throw new IllegalArgumentException("Peon introuvable: " + peonId); }
        return world.getPeons().get(peonId).getMentalMap();
    }

    @GetMapping(path = "/{worldId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID worldId) { return this.broadcaster.subscribe(worldId); }
}
