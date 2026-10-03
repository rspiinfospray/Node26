package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GetWorldStateCommand {
    private final Illuvatar illuvatar;
    public GetWorldStateCommand(Illuvatar illuvatar) { this.illuvatar = illuvatar; }
    public World execute(UUID worldId, Long sequenceNumber) { return sequenceNumber == null ? this.illuvatar.getWorld(worldId) : this.illuvatar.getWorldAtSequence(worldId, sequenceNumber); }
}
