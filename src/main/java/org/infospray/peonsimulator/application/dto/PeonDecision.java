package org.infospray.peonsimulator.application.dto;

import org.infospray.peonsimulator.domain.action.PeonAction;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.HexCoordinate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PeonDecision(PeonAction action, String situationKey, double selectedScore, boolean explorationChoice, List<CandidateScore> candidates) {
    public record CandidateScore(ActionType actionType, HexCoordinate destination, UUID targetPeonId, double score, Map<String, Double> factors) {
    }
}
