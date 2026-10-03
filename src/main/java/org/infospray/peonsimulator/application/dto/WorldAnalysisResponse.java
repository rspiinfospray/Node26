package org.infospray.peonsimulator.application.dto;

import org.infospray.peonsimulator.domain.model.WorldStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorldAnalysisResponse(UUID worldId, WorldStatus status, int currentRound, int maxRounds, long sequenceNumber, int totalEvents, long totalActions, int totalPeons, int alivePeons, int deadPeons, Map<String, Long> actionsByType, Map<String, Long> decisionsByReason, Map<String, Long> eventsByType, Map<String, Long> deathsByReason, Map<String, Long> terrainByType, List<TeamAnalysis> teams, List<String> behavioralTrends) {
    public record TeamAnalysis(UUID teamId, String name, String color, int totalPeons, int alivePeons, int deadPeons, double averageLevel, double averageExperience, double averageHealthOfLiving, long actions, long moves, long meals, long attacks, long communications) {
    }
}
