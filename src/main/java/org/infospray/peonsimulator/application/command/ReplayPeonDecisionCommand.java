package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReplayPeonDecisionCommand {
    private final Illuvatar illuvatar;
    public ReplayPeonDecisionCommand(Illuvatar illuvatar) { this.illuvatar = illuvatar; }
    public PeonDecision execute(UUID worldId, UUID peonId, long sequenceNumber) { return this.illuvatar.replayDecision(worldId, peonId, sequenceNumber); }
}
