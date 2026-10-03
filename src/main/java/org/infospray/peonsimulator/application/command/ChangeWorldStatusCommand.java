package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.domain.model.World;
import org.infospray.peonsimulator.domain.model.WorldStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChangeWorldStatusCommand {
    private final Illuvatar illuvatar;
    public ChangeWorldStatusCommand(Illuvatar illuvatar) { this.illuvatar = illuvatar; }
    public World execute(UUID worldId, WorldStatus status) { return this.illuvatar.setStatus(worldId, status); }
}
