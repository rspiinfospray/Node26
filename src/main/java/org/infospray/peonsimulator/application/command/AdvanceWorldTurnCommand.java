package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdvanceWorldTurnCommand {
    private final Illuvatar illuvatar;
    public AdvanceWorldTurnCommand(Illuvatar illuvatar) { this.illuvatar = illuvatar; }
    public World execute(UUID worldId, boolean manual) { return this.illuvatar.advance(worldId, manual); }
}
