package org.infospray.peonsimulator.domain.action;

import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.PeonPurpose;

import java.util.UUID;

public record PeonAction(ActionType type, HexCoordinate destination, UUID targetPeonId, PeonPurpose purpose, String reason) {
    public static PeonAction idle(PeonPurpose purpose, String reason) {
        return new PeonAction(ActionType.NE_RIEN_FAIRE, null, null, purpose, reason);
    }
}
