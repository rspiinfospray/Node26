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
import org.infospray.peonsimulator.application.command.GetPeonDiagnosticCommand;
import org.infospray.peonsimulator.application.command.GetPeonSurroundingsCommand;
import org.infospray.peonsimulator.application.command.ReplayPeonDecisionCommand;
import org.infospray.peonsimulator.application.command.SummarizePeonDiagnosticCommand;
import org.infospray.peonsimulator.application.command.GenerateWorldBehaviorReportCommand;
import org.infospray.peonsimulator.application.command.GetWorldBehaviorReportCommand;
import org.infospray.peonsimulator.application.command.QueueWorldSupervisionCommand;
import org.infospray.peonsimulator.application.dto.CreateWorldRequest;
import org.infospray.peonsimulator.application.dto.EventQuery;
import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDiagnosticResponse;
import org.infospray.peonsimulator.application.dto.PeonDiagnosticSummary;
import org.infospray.peonsimulator.application.dto.WorldAnalysisResponse;
import org.infospray.peonsimulator.application.dto.WorldBehaviorReportResponse;
import org.infospray.peonsimulator.application.dto.WorldBehaviorReportStatus;
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
import java.util.Set;

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
    private final GetPeonDiagnosticCommand getPeonDiagnosticCommand;
    private final SummarizePeonDiagnosticCommand summarizePeonDiagnosticCommand;
    private final GetPeonSurroundingsCommand getPeonSurroundingsCommand;
    private final ReplayPeonDecisionCommand replayPeonDecisionCommand;
    private final GenerateWorldBehaviorReportCommand generateWorldBehaviorReportCommand;
    private final GetWorldBehaviorReportCommand getWorldBehaviorReportCommand;
    private final QueueWorldSupervisionCommand queueWorldSupervisionCommand;
    private final SimulationEventBroadcaster broadcaster;

    public WorldController(
            Illuvatar illuvatar, CreateWorldCommand createWorldCommand, AdvanceWorldTurnCommand advanceWorldTurnCommand, ChangeWorldStatusCommand changeWorldStatusCommand,
            GetWorldStateCommand getWorldStateCommand, GetWorldTimelineCommand getWorldTimelineCommand, AnalyzeWorldCommand analyzeWorldCommand,
            DeleteAllWorldsCommand deleteAllWorldsCommand, GetPeonDiagnosticCommand getPeonDiagnosticCommand, SummarizePeonDiagnosticCommand summarizePeonDiagnosticCommand,
            GetPeonSurroundingsCommand getPeonSurroundingsCommand, ReplayPeonDecisionCommand replayPeonDecisionCommand, GenerateWorldBehaviorReportCommand generateWorldBehaviorReportCommand,
            GetWorldBehaviorReportCommand getWorldBehaviorReportCommand, QueueWorldSupervisionCommand queueWorldSupervisionCommand, SimulationEventBroadcaster broadcaster) {
        this.illuvatar = illuvatar;
        this.createWorldCommand = createWorldCommand;
        this.advanceWorldTurnCommand = advanceWorldTurnCommand;
        this.changeWorldStatusCommand = changeWorldStatusCommand;
        this.getWorldStateCommand = getWorldStateCommand;
        this.getWorldTimelineCommand = getWorldTimelineCommand;
        this.analyzeWorldCommand = analyzeWorldCommand;
        this.deleteAllWorldsCommand = deleteAllWorldsCommand;
        this.getPeonDiagnosticCommand = getPeonDiagnosticCommand;
        this.summarizePeonDiagnosticCommand = summarizePeonDiagnosticCommand;
        this.getPeonSurroundingsCommand = getPeonSurroundingsCommand;
        this.replayPeonDecisionCommand = replayPeonDecisionCommand;
        this.generateWorldBehaviorReportCommand = generateWorldBehaviorReportCommand;
        this.getWorldBehaviorReportCommand = getWorldBehaviorReportCommand;
        this.queueWorldSupervisionCommand = queueWorldSupervisionCommand;
        this.broadcaster = broadcaster;
    }

    @PostMapping
    public World create(@Valid @RequestBody CreateWorldRequest request) { return this.createWorldCommand.execute(request); }

    @GetMapping
    public List<World> list() { return this.illuvatar.getWorlds(); }

    @GetMapping("/diagnostics/peon")
    public PeonDiagnosticResponse diagnosticByName(@RequestParam String world, @RequestParam String peon, @RequestParam(required = false) Integer fromRound, @RequestParam(required = false) Integer toRound) {
        World selectedWorld = this.illuvatar.getWorlds().stream().filter(candidate -> candidate.getName().equalsIgnoreCase(world)).findFirst().orElseThrow(() -> new IllegalArgumentException("Monde introuvable: " + world));
        UUID peonId = selectedWorld.getPeons().values().stream().filter(candidate -> candidate.getFirstName().equalsIgnoreCase(peon)).map(org.infospray.peonsimulator.domain.model.Peon::getId).findFirst().orElseThrow(() -> new IllegalArgumentException("Peon introuvable: " + peon));
        return this.getPeonDiagnosticCommand.execute(selectedWorld.getId(), peonId, fromRound, toRound);
    }

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
    public List<SimulationEvent> events(@PathVariable UUID worldId, @RequestParam(defaultValue = "0") long after, @RequestParam(defaultValue = "500") int limit, @RequestParam(required = false) UUID peonId, @RequestParam(required = false) Integer fromRound, @RequestParam(required = false) Integer toRound, @RequestParam(required = false) Set<String> types) { return this.getWorldTimelineCommand.execute(new EventQuery(worldId, after, limit, peonId, fromRound, toRound, types)); }

    @GetMapping("/{worldId}/peons/{peonId}/diagnostic")
    public PeonDiagnosticResponse diagnostic(@PathVariable UUID worldId, @PathVariable UUID peonId, @RequestParam(required = false) Integer fromRound, @RequestParam(required = false) Integer toRound) { return this.getPeonDiagnosticCommand.execute(worldId, peonId, fromRound, toRound); }

    @GetMapping("/{worldId}/peons/{peonId}/diagnostic/summary")
    public PeonDiagnosticSummary diagnosticSummary(@PathVariable UUID worldId, @PathVariable UUID peonId, @RequestParam(required = false) Integer fromRound, @RequestParam(required = false) Integer toRound) { return this.summarizePeonDiagnosticCommand.execute(worldId, peonId, fromRound, toRound); }

    @GetMapping("/{worldId}/peons/{peonId}/surroundings")
    public Map<String, Object> surroundings(@PathVariable UUID worldId, @PathVariable UUID peonId, @RequestParam long sequence, @RequestParam(defaultValue = "6") int radius) { return this.getPeonSurroundingsCommand.execute(worldId, peonId, sequence, radius); }

    @PostMapping("/{worldId}/peons/{peonId}/replay-decision")
    public PeonDecision replayDecision(@PathVariable UUID worldId, @PathVariable UUID peonId, @RequestParam long sequence) { return this.replayPeonDecisionCommand.execute(worldId, peonId, sequence); }

    @GetMapping("/{worldId}/analysis")
    public WorldAnalysisResponse analysis(@PathVariable UUID worldId) { return this.analyzeWorldCommand.execute(worldId); }

    @GetMapping("/{worldId}/behavior-report")
    public WorldBehaviorReportResponse behaviorReport(@PathVariable UUID worldId) { return this.getWorldBehaviorReportCommand.execute(worldId); }

    @PostMapping("/{worldId}/behavior-report")
    public WorldBehaviorReportStatus generateBehaviorReport(@PathVariable UUID worldId) { return this.generateWorldBehaviorReportCommand.execute(worldId); }

    @PostMapping("/{worldId}/supervision")
    public WorldBehaviorReportStatus startSupervision(@PathVariable UUID worldId) {
        this.generateWorldBehaviorReportCommand.execute(worldId);
        return this.queueWorldSupervisionCommand.execute(worldId);
    }

    @GetMapping("/{worldId}/peons/{peonId}/mental-map")
    public Map<String, RememberedCell> mentalMap(@PathVariable UUID worldId, @PathVariable UUID peonId, @RequestParam(required = false) Long sequence) {
        World world = this.getWorldStateCommand.execute(worldId, sequence);
        if (!world.getPeons().containsKey(peonId)) { throw new IllegalArgumentException("Peon introuvable: " + peonId); }
        return world.getPeons().get(peonId).getMentalMap();
    }

    @GetMapping(path = "/{worldId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID worldId) { return this.broadcaster.subscribe(worldId); }
}
