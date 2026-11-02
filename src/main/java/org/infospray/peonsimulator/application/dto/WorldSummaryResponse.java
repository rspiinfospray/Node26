package org.infospray.peonsimulator.application.dto;

import org.infospray.peonsimulator.domain.model.WorldStatus;

import java.util.UUID;

public record WorldSummaryResponse(UUID id, String name, WorldStatus status, int currentRound, int maxRounds, long sequenceNumber, int totalPeons, int alivePeons) {
}
