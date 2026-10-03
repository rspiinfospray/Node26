package org.infospray.peonsimulator.application.dto;

import org.infospray.peonsimulator.domain.action.PeonAction;

import java.util.UUID;

public record ActionRequest(UUID actionId, UUID worldId, UUID peonId, long sequenceNumber, PeonAction action) {
}
