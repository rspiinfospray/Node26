package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.WorldAnalysisResponse;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.Team;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AnalyzeWorldCommand {
    private final WorldRepository worldRepository;
    private final EventRepository eventRepository;

    public AnalyzeWorldCommand(WorldRepository worldRepository, EventRepository eventRepository) {
        this.worldRepository = worldRepository;
        this.eventRepository = eventRepository;
    }

    public WorldAnalysisResponse execute(UUID worldId) {
        World world = this.worldRepository.findById(worldId).orElseThrow(() -> new IllegalArgumentException("Monde introuvable: " + worldId));
        List<SimulationEvent> events = this.eventRepository.findByWorldId(worldId);
        Map<String, Long> eventsByType = this.countEventsByType(events);
        Map<String, Long> actionsByType = this.countPayloadValues(events, "PEON_DECISION_MADE", "actionType");
        Map<String, Long> decisionsByReason = this.countPayloadValues(events, "PEON_DECISION_MADE", "reason");
        Map<String, Long> deathsByReason = this.countPayloadValues(events, "PEON_DIED", "reason");
        Map<String, Long> terrainByType = new LinkedHashMap<>();
        world.getCells().values().forEach(cell -> terrainByType.merge(cell.getTerrain().name(), 1L, Long::sum));
        int alivePeons = (int) world.getPeons().values().stream().filter(Peon::isAlive).count();
        long totalActions = eventsByType.getOrDefault("PEON_DECISION_MADE", 0L);
        List<WorldAnalysisResponse.TeamAnalysis> teams = world.getTeams().values().stream().map(team -> this.analyzeTeam(world, team, events)).toList();
        List<String> trends = this.buildTrends(world, totalActions, actionsByType, decisionsByReason, alivePeons);
        return new WorldAnalysisResponse(worldId, world.getStatus(), world.getCurrentRound(), world.getMaxRounds(), world.getSequenceNumber(), events.size(), totalActions, world.getPeons().size(), alivePeons, world.getPeons().size() - alivePeons, actionsByType, decisionsByReason, eventsByType, deathsByReason, terrainByType, teams, trends);
    }

    private Map<String, Long> countEventsByType(List<SimulationEvent> events) {
        Map<String, Long> counts = new LinkedHashMap<>();
        events.forEach(event -> counts.merge(event.eventType(), 1L, Long::sum));
        return counts;
    }

    private Map<String, Long> countPayloadValues(List<SimulationEvent> events, String eventType, String payloadKey) {
        Map<String, Long> counts = new LinkedHashMap<>();
        events.stream().filter(event -> eventType.equals(event.eventType())).map(event -> event.payload().get(payloadKey)).filter(value -> value != null).map(String::valueOf).forEach(value -> counts.merge(value, 1L, Long::sum));
        return counts;
    }

    private WorldAnalysisResponse.TeamAnalysis analyzeTeam(World world, Team team, List<SimulationEvent> events) {
        List<Peon> peons = world.getPeons().values().stream().filter(peon -> team.id().equals(peon.getTeamId())).toList();
        List<SimulationEvent> teamEvents = events.stream().filter(event -> team.id().equals(event.teamId())).toList();
        int alive = (int) peons.stream().filter(Peon::isAlive).count();
        double averageLevel = peons.stream().mapToInt(Peon::getLevel).average().orElse(0);
        double averageExperience = peons.stream().mapToInt(Peon::getExperiencePoints).average().orElse(0);
        double averageHealth = peons.stream().filter(Peon::isAlive).mapToInt(Peon::getHealthPoints).average().orElse(0);
        return new WorldAnalysisResponse.TeamAnalysis(team.id(), team.name(), team.color(), peons.size(), alive, peons.size() - alive, this.round(averageLevel), this.round(averageExperience), this.round(averageHealth), this.count(teamEvents, "PEON_DECISION_MADE"), this.count(teamEvents, "PEON_MOVED"), this.count(teamEvents, "PEON_ATE"), this.count(teamEvents, "PEON_ATTACKED"), this.count(teamEvents, "PEON_COMMUNICATED"));
    }

    private long count(List<SimulationEvent> events, String eventType) {
        return events.stream().filter(event -> eventType.equals(event.eventType())).count();
    }

    private List<String> buildTrends(World world, long totalActions, Map<String, Long> actions, Map<String, Long> reasons, int alivePeons) {
        List<String> trends = new ArrayList<>();
        long moves = actions.getOrDefault("SE_DEPLACER", 0L);
        long meals = actions.getOrDefault("MANGER", 0L);
        long attacks = actions.getOrDefault("ATTAQUER", 0L);
        long communications = actions.getOrDefault("COMMUNIQUER", 0L);
        if (totalActions == 0) trends.add("La simulation n’a encore produit aucune décision de peon.");
        if (totalActions > 0 && moves * 2 >= totalActions) trends.add("L’exploration domine : au moins une décision sur deux est un déplacement.");
        if (totalActions > 0 && meals * 4 >= totalActions) trends.add("La recherche de nourriture occupe une part importante des décisions.");
        if (attacks == 0) trends.add("Aucun combat n’a encore eu lieu entre les équipes.");
        if (communications == 0) trends.add("Les peons n’ont pas encore partagé leur carte mentale avec leurs alliés.");
        long pursuits = reasons.entrySet().stream().filter(entry -> entry.getKey().toLowerCase().contains("ennemi") || entry.getKey().toLowerCase().contains("poursuit")).mapToLong(Map.Entry::getValue).sum();
        if (pursuits > 0 && attacks == 0) trends.add("Des ennemis sont repérés ou poursuivis, mais ces poursuites ne débouchent pas encore sur des attaques.");
        if (alivePeons == world.getPeons().size() && !world.getPeons().isEmpty()) trends.add("Tous les peons créés sont encore vivants.");
        if (alivePeons == 0 && !world.getPeons().isEmpty()) trends.add("Aucun peon n’a survécu à la simulation.");
        int minimumLevel = world.getPeons().values().stream().mapToInt(Peon::getLevel).min().orElse(0);
        int maximumLevel = world.getPeons().values().stream().mapToInt(Peon::getLevel).max().orElse(0);
        if (!world.getPeons().isEmpty() && maximumLevel - minimumLevel <= 1) trends.add("La progression en niveau reste homogène entre les peons.");
        return trends;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
