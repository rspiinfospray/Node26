package org.infospray.peonsimulator.domain.model;

public record ActionExperience(long sequenceNumber, ActionType actionType, String situationKey, double reward, int healthDelta, int experienceDelta, int knownCellsDelta, boolean rejected, boolean survived) {
}
