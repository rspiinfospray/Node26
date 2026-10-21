package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.PeonBehaviorAnalysisRequest;
import org.infospray.peonsimulator.application.dto.PeonDiagnosticSummary;
import org.infospray.peonsimulator.application.dto.WorldBehaviorReportStatus;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.PeonBehaviorAnalyst;
import org.infospray.peonsimulator.application.port.secondary.WorldBehaviorReportRepository;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.World;
import org.infospray.peonsimulator.domain.model.WorldStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GenerateWorldBehaviorReportCommand {
    private final WorldRepository worldRepository;
    private final EventRepository eventRepository;
    private final WorldBehaviorReportRepository reportRepository;
    private final PeonBehaviorAnalyst behaviorAnalyst;
    private final GameDefaultsProperties defaults;

    public GenerateWorldBehaviorReportCommand(WorldRepository worldRepository, EventRepository eventRepository, WorldBehaviorReportRepository reportRepository, PeonBehaviorAnalyst behaviorAnalyst, GameDefaultsProperties defaults) {
        this.worldRepository = worldRepository;
        this.eventRepository = eventRepository;
        this.reportRepository = reportRepository;
        this.behaviorAnalyst = behaviorAnalyst;
        this.defaults = defaults;
    }

    public synchronized WorldBehaviorReportStatus execute(UUID worldId) {
        World world = this.worldRepository.findById(worldId).orElseThrow(() -> new IllegalArgumentException("Monde introuvable: " + worldId));
        if (world.getStatus() != WorldStatus.FINISHED) { throw new IllegalStateException("Le rapport ne peut être généré que pour un monde terminé"); }
        WorldBehaviorReportStatus existing = this.reportRepository.status(worldId);
        if (existing.status() == WorldBehaviorReportStatus.Status.GENERATED || existing.status() == WorldBehaviorReportStatus.Status.GENERATING) { return existing; }
        this.reportRepository.markGenerating(worldId);
        try {
            List<SimulationEvent> events = this.eventRepository.findByWorldId(worldId);
            String markdown = this.generateMarkdown(world, events);
            this.reportRepository.save(worldId, world.getName(), markdown);
            return this.reportRepository.status(worldId);
        } catch (RuntimeException exception) {
            this.reportRepository.markFailed(worldId, this.safeError(exception));
            throw exception;
        }
    }

    private String generateMarkdown(World world, List<SimulationEvent> events) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Analyse comportementale du monde ").append(world.getName()).append("\n\n");
        markdown.append("- Identifiant : `").append(world.getId()).append("`\n");
        markdown.append("- Fin de simulation : tour ").append(world.getCurrentRound()).append(" sur ").append(world.getMaxRounds()).append("\n");
        markdown.append("- Péons : ").append(world.getPeons().size()).append(", survivants : ").append(world.alivePeons().size()).append("\n");
        markdown.append("- Événements analysés : ").append(events.size()).append("\n");
        markdown.append("- Généré le : ").append(Instant.now()).append("\n\n");
        Map<UUID, List<SimulationEvent>> eventsByPeon = events.stream().filter(event -> event.peonId() != null).collect(Collectors.groupingBy(SimulationEvent::peonId));
        for (Peon peon : world.getPeons().values()) {
            List<SimulationEvent> peonEvents = eventsByPeon.getOrDefault(peon.getId(), List.of());
            PeonDiagnosticSummary summary = this.summary(world.getId(), peon.getId(), peonEvents);
            PeonBehaviorAnalysisRequest request = new PeonBehaviorAnalysisRequest(this.worldContext(world, events), this.gameRules(), peon, peonEvents, summary);
            markdown.append("## ").append(peon.getFirstName()).append("\n\n");
            markdown.append(this.behaviorAnalyst.analyze(request).strip()).append("\n\n");
        }
        return markdown.toString();
    }

    private Map<String, Object> worldContext(World world, List<SimulationEvent> events) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", world.getId());
        context.put("name", world.getName());
        context.put("dimensions", Map.of("width", world.getWidth(), "height", world.getHeight()));
        context.put("generation", Map.of("seed", world.getSeed(), "rockPercentage", world.getRockPercentage(), "treePercentage", world.getTreePercentage(), "foodCellPercentage", world.getFoodCellPercentage(), "initialSheep", world.getSheepCount()));
        context.put("endState", Map.of("round", world.getCurrentRound(), "maxRounds", world.getMaxRounds(), "sequence", world.getSequenceNumber(), "alivePeons", world.alivePeons().size(), "totalPeons", world.getPeons().size()));
        context.put("teams", world.getTeams());
        context.put("houses", world.getHouses());
        context.put("granaries", world.getGranaries());
        context.put("fields", world.getWheatFields());
        context.put("remainingSheep", world.getSheep());
        context.put("eventCounts", this.eventCounts(events));
        return context;
    }

    private Map<String, Object> gameRules() {
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("hungerHealthLossPerTurn", this.defaults.getHungerHealthLossPerTurn());
        rules.put("moveHealthCost", this.defaults.getMoveHealthCost());
        rules.put("berries", Map.of("healthGain", this.defaults.getEatHealthGain(), "experienceGain", this.defaults.getEatExperienceGain()));
        rules.put("sheep", Map.of("healthGain", this.defaults.getSheepHealthGain(), "experienceGain", this.defaults.getSheepExperienceGain(), "movementPercentage", this.defaults.getSheepMovementPercentage()));
        rules.put("wheat", Map.of("healthGain", this.defaults.getWheatHealthGain(), "experienceGain", this.defaults.getWheatExperienceGain()));
        rules.put("agriculture", Map.of("woodForGranary", this.defaults.getWoodRequiredForGranary(), "woodForField", this.defaults.getWoodRequiredForField(), "granaryInfluenceRadius", this.defaults.getConstructionInfluenceRadius(), "growthRounds", this.defaults.getFieldGrowthRounds(), "yield", this.defaults.getFieldWheatYield(), "granaryCapacity", this.defaults.getGranaryCapacity()));
        rules.put("house", Map.of("woodRequired", this.defaults.getWoodRequiredForHouse(), "hungerPercentage", this.defaults.getHouseHungerPercentage(), "ownership", "Une maison reste réservée à ses propriétaires vivants et aux alliés invités"));
        rules.put("decisionThresholds", Map.of("eatHealth", this.defaults.getEatHealthThreshold(), "seekFoodHealth", this.defaults.getSeekFoodHealthThreshold(), "attackMinimumHealth", this.defaults.getAttackMinimumHealth(), "fleeHealth", this.defaults.getEnemyFleeHealthThreshold(), "pursuitHealth", this.defaults.getEnemyPursuitHealthThreshold()));
        rules.put("longTermStrategy", "Stratégie persistante réévaluée périodiquement; elle bonifie les décisions immédiates sans empêcher une urgence de survie");
        return rules;
    }

    private PeonDiagnosticSummary summary(UUID worldId, UUID peonId, List<SimulationEvent> events) {
        List<PeonDiagnosticSummary.Anomaly> anomalies = new ArrayList<>();
        List<Integer> criticalRounds = events.stream().filter(event -> event.eventType().equals("PEON_DECISION_MADE")).filter(event -> String.valueOf(event.payload().get("situationKey")).startsWith("PV_CRITIQUES")).filter(event -> List.of("VOIR", "NE_RIEN_FAIRE").contains(String.valueOf(event.payload().get("actionType")))).map(SimulationEvent::roundNumber).toList();
        if (!criticalRounds.isEmpty()) { anomalies.add(new PeonDiagnosticSummary.Anomaly("NON_SURVIVAL_ACTION_AT_CRITICAL_HEALTH", criticalRounds, Map.of("count", criticalRounds.size()))); }
        events.stream().filter(event -> event.eventType().equals("PEON_DIED")).forEach(event -> anomalies.add(new PeonDiagnosticSummary.Anomaly("DEATH", List.of(event.roundNumber()), Map.of("sequence", event.sequenceNumber(), "details", event.payload()))));
        this.detectOscillation(events, anomalies);
        return new PeonDiagnosticSummary(worldId, peonId, anomalies);
    }

    private void detectOscillation(List<SimulationEvent> events, List<PeonDiagnosticSummary.Anomaly> anomalies) {
        List<String> destinations = events.stream().filter(event -> event.eventType().equals("PEON_DECISION_MADE")).map(event -> event.payload().get("destination")).filter(java.util.Objects::nonNull).map(Object::toString).toList();
        for (int index = 3; index < destinations.size(); index++) {
            if (destinations.get(index).equals(destinations.get(index - 2)) && destinations.get(index - 1).equals(destinations.get(index - 3))) { anomalies.add(new PeonDiagnosticSummary.Anomaly("MOVEMENT_OSCILLATION", List.of(), Map.of("positions", destinations.subList(index - 3, index + 1)))); return; }
        }
    }

    private Map<String, Long> eventCounts(List<SimulationEvent> events) {
        Map<String, Long> counts = new LinkedHashMap<>();
        events.forEach(event -> counts.merge(event.eventType(), 1L, Long::sum));
        return counts;
    }

    private String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message.substring(0, Math.min(message.length(), 500));
    }
}
