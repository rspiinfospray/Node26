package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.application.dto.CreateWorldRequest;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

@Component
public class CreateWorldCommand {
    private final Illuvatar illuvatar;
    public CreateWorldCommand(Illuvatar illuvatar) { this.illuvatar = illuvatar; }
    public World execute(CreateWorldRequest request) { return this.illuvatar.createWorld(request); }
}
